package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.common.exception.ErrorCode;
import com.pickupexpress.common.util.TenantUtil;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.LiftRecord;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.entity.SettlementOrder;
import com.pickupexpress.domain.enums.SettlementStatusEnum;
import com.pickupexpress.domain.vo.SettlementVO;
import com.pickupexpress.mapper.ContractMapper;
import com.pickupexpress.mapper.LiftRecordMapper;
import com.pickupexpress.mapper.PickupOrderMapper;
import com.pickupexpress.mapper.SettlementOrderMapper;
import com.pickupexpress.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private final LiftRecordMapper liftRecordMapper;

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

        List<LiftRecord> lifts = liftRecordMapper.selectList(
                new LambdaQueryWrapper<LiftRecord>().eq(LiftRecord::getPickupOrderId, pickupOrderId));
        BigDecimal totalWeight = lifts.stream()
                .map(l -> l.getActualWeight() != null ? l.getActualWeight() : l.getTheoreticalWeight() != null ? l.getTheoreticalWeight() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalWithTax = totalAmount.add(taxAmount);
        BigDecimal deductedPrepayment = BigDecimal.ZERO;
        BigDecimal receivableAmount = totalWithTax.subtract(deductedPrepayment);

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
                .pdfUrl(null)
                .customerViewed(0)
                .syncedToRecon(0)
                .status(SettlementStatusEnum.SETTLED.getValue())
                .build();
        save(settlement);

        order.setSettlementStatus(SettlementStatusEnum.SETTLED.getValue());
        pickupOrderMapper.updateById(order);

        return settlement.getId();
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
}
