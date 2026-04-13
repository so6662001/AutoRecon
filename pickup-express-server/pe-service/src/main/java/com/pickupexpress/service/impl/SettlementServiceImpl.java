package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.common.exception.ErrorCode;
import com.pickupexpress.common.result.PageResult;
import com.pickupexpress.common.util.TenantUtil;
import com.pickupexpress.common.util.SecurityUtil;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.LiftRecord;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.entity.SettlementOrder;
import com.pickupexpress.domain.enums.SettlementStatusEnum;
import com.pickupexpress.domain.vo.SettlementVO;
import com.pickupexpress.domain.entity.ContractItem;
import com.pickupexpress.mapper.ContractItemMapper;
import com.pickupexpress.mapper.ContractMapper;
import com.pickupexpress.mapper.LiftRecordMapper;
import com.pickupexpress.mapper.PickupOrderMapper;
import com.pickupexpress.mapper.SettlementOrderMapper;
import com.pickupexpress.service.NotificationService;
import com.pickupexpress.service.ProgressEventService;
import com.pickupexpress.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 结算服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl extends ServiceImpl<SettlementOrderMapper, SettlementOrder> implements SettlementService {

    private static final String SETTLEMENT_NO_PREFIX = "JS";

    private final PickupOrderMapper pickupOrderMapper;
    private final ContractMapper contractMapper;
    private final ContractItemMapper contractItemMapper;
    private final LiftRecordMapper liftRecordMapper;
    private final ProgressEventService progressEventService;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateSettlement(Long pickupOrderId) {
        PickupOrder order = pickupOrderMapper.selectById(pickupOrderId);
        if (order == null) {
            throw new BizException(ErrorCode.PICKUP_ORDER_NOT_FOUND);
        }
        Contract contract = contractMapper.selectById(order.getContractId());
        if (contract != null) {
            TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        }

        // 获取逐吊发货记录
        List<LiftRecord> lifts = liftRecordMapper.selectList(
                new LambdaQueryWrapper<LiftRecord>().eq(LiftRecord::getPickupOrderId, pickupOrderId));

        // 获取合同明细(用于获取单价)
        List<ContractItem> contractItems = order.getContractId() != null
                ? contractItemMapper.selectList(new LambdaQueryWrapper<ContractItem>().eq(ContractItem::getContractId, order.getContractId()))
                : List.of();

        // 按合同单价计算每条明细金额(设计文档4.7: 按合同价格×实际重量)
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Map<String, Object>> settlementDetail = new ArrayList<>();

        for (LiftRecord lift : lifts) {
            BigDecimal weight = lift.getActualWeight() != null ? lift.getActualWeight()
                    : lift.getTheoreticalWeight() != null ? lift.getTheoreticalWeight() : BigDecimal.ZERO;
            totalWeight = totalWeight.add(weight);

            // 查找对应品规的合同单价
            BigDecimal unitPrice = findUnitPrice(contractItems, lift.getProductName(), lift.getSpec());
            BigDecimal lineAmount = weight.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(lineAmount);

            // 结算明细
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("liftSeq", lift.getLiftSeq());
            detail.put("productName", lift.getProductName());
            detail.put("spec", lift.getSpec());
            detail.put("material", lift.getMaterial());
            detail.put("pieces", lift.getPieces());
            detail.put("weight", weight);
            detail.put("unitPrice", unitPrice);
            detail.put("amount", lineAmount);
            detail.put("dataSource", lift.getDataSource());
            settlementDetail.add(detail);
        }

        // 计算税额(默认13%增值税)
        BigDecimal taxRate = BigDecimal.valueOf(0.13);
        BigDecimal taxAmount = totalAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalWithTax = totalAmount.add(taxAmount);

        // 扣减已付款/预付款
        BigDecimal deductedPrepayment = contract != null && contract.getPaidAmount() != null
                ? contract.getPaidAmount() : BigDecimal.ZERO;
        // 实际应收 = 价税合计 - 已扣预付(但不低于0)
        BigDecimal receivableAmount = totalWithTax.subtract(deductedPrepayment).max(BigDecimal.ZERO);

        // 结算明细JSON
        String detailJson = null;
        try {
            detailJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(settlementDetail);
        } catch (Exception e) {
            log.warn("Failed to serialize settlement detail: {}", e.getMessage());
        }

        String settlementNo = generateSettlementNo();

        SettlementOrder settlement = SettlementOrder.builder()
                .settlementNo(settlementNo)
                .pickupOrderId(pickupOrderId)
                .contractId(order.getContractId())
                .contractNo(order.getContractNo())
                .buyerId(order.getBuyerId())
                .totalWeight(totalWeight)
                .totalAmount(totalAmount)
                .taxAmount(taxAmount)
                .totalWithTax(totalWithTax)
                .deductedPrepayment(deductedPrepayment)
                .receivableAmount(receivableAmount)
                .settlementDetail(detailJson)
                .pdfUrl(null) // TODO: PDF 生成
                .customerViewed(0)
                .syncedToRecon(0)
                .status(SettlementStatusEnum.SETTLED.getValue())
                .build();
        save(settlement);

        // 更新提货单结算状态和金额
        order.setSettlementStatus(SettlementStatusEnum.SETTLED.getValue());
        order.setTotalAmount(totalWithTax);
        pickupOrderMapper.updateById(order);

        // 进度事件: 结算单生成
        progressEventService.recordEvent(order.getId(), order.getContractId(),
                "SETTLEMENT_CREATED",
                "结算单" + settlementNo + "已生成: 重量" + totalWeight.toPlainString() + "吨, 金额¥" + totalWithTax.toPlainString(),
                null, "system");

        // 通知客户(15.2原则: 友好用语, 事后通知)
        try {
            notificationService.sendSettlementNotification(settlement.getId());
        } catch (Exception e) {
            log.warn("Failed to send settlement notification: {}", e.getMessage());
        }

        log.info("Generated settlement: no={}, weight={}, amount={}, receivable={}",
                settlementNo, totalWeight, totalWithTax, receivableAmount);
        return settlement.getId();
    }

    /**
     * 根据品规查找合同单价
     */
    private BigDecimal findUnitPrice(List<ContractItem> items, String productName, String spec) {
        for (ContractItem item : items) {
            boolean nameMatch = productName != null && productName.equals(item.getProductName());
            boolean specMatch = spec != null && spec.equals(item.getSpec());
            if (nameMatch && specMatch && item.getUnitPrice() != null) {
                return item.getUnitPrice();
            }
        }
        // 只匹配品名
        for (ContractItem item : items) {
            if (productName != null && productName.equals(item.getProductName()) && item.getUnitPrice() != null) {
                return item.getUnitPrice();
            }
        }
        // 取第一个有单价的
        for (ContractItem item : items) {
            if (item.getUnitPrice() != null) return item.getUnitPrice();
        }
        return BigDecimal.ZERO;
    }

    private String generateSettlementNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = count(new LambdaQueryWrapper<SettlementOrder>()
                .likeRight(SettlementOrder::getSettlementNo, SETTLEMENT_NO_PREFIX + dateStr));
        return SETTLEMENT_NO_PREFIX + dateStr + String.format("%04d", count + 1);
    }

    @Override
    public SettlementVO getSettlement(Long settlementId) {
        SettlementOrder settlement = getById(settlementId);
        if (settlement == null) {
            throw new BizException(ErrorCode.SETTLEMENT_NOT_FOUND);
        }
        Contract contract = contractMapper.selectById(settlement.getContractId());
        if (contract != null) {
            TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        }
        SettlementVO vo = new SettlementVO();
        BeanUtils.copyProperties(settlement, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markCustomerViewed(Long settlementId) {
        SettlementOrder settlement = getById(settlementId);
        if (settlement == null) {
            throw new BizException(ErrorCode.SETTLEMENT_NOT_FOUND);
        }
        Contract contract = contractMapper.selectById(settlement.getContractId());
        if (contract != null) {
            TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        }
        settlement.setCustomerViewed(1);
        settlement.setCustomerViewedAt(java.time.LocalDateTime.now());
        updateById(settlement);
    }

    @Override
    public List<SettlementOrder> listByContract(Long contractId) {
        return list(new LambdaQueryWrapper<SettlementOrder>().eq(SettlementOrder::getContractId, contractId));
    }

    @Override
    public PageResult<SettlementVO> listSettlements(String settlementNo, Long contractId, Integer status,
            Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<SettlementOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(settlementNo != null && !settlementNo.isBlank(), SettlementOrder::getSettlementNo, settlementNo);
        wrapper.eq(contractId != null, SettlementOrder::getContractId, contractId);
        wrapper.eq(status != null, SettlementOrder::getStatus, status);

        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId != null) {
            List<Long> contractIds = contractMapper.selectList(
                    new LambdaQueryWrapper<Contract>()
                            .eq(Contract::getSellerId, currentEnterpriseId)
                            .or()
                            .eq(Contract::getBuyerId, currentEnterpriseId))
                    .stream().map(Contract::getId).toList();
            if (!contractIds.isEmpty()) {
                wrapper.in(SettlementOrder::getContractId, contractIds);
            } else {
                wrapper.eq(SettlementOrder::getId, -1L);
            }
        }

        wrapper.orderByDesc(SettlementOrder::getCreatedAt);
        int pn = pageNum != null && pageNum > 0 ? pageNum : 1;
        int ps = pageSize != null && pageSize > 0 ? pageSize : 20;
        IPage<SettlementOrder> page = page(new Page<>(pn, ps), wrapper);

        List<SettlementVO> voList = page.getRecords().stream().map(s -> {
            SettlementVO vo = new SettlementVO();
            BeanUtils.copyProperties(s, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, page.getTotal(), page.getSize(), page.getCurrent(), page.getPages());
    }
}
