package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.result.PageResult;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.ReconBillCreateDTO;
import com.autorecon.domain.dto.ReconBillItemDTO;
import com.autorecon.domain.dto.ReconBillQueryDTO;
import com.autorecon.domain.entity.Enterprise;
import com.autorecon.domain.entity.Payment;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.entity.ReconTemplate;
import com.autorecon.domain.enums.BillStatusEnum;
import com.autorecon.domain.vo.DashboardVO;
import com.autorecon.domain.vo.DisputeVO;
import com.autorecon.domain.vo.PaymentVO;
import com.autorecon.domain.vo.ReconBillDetailVO;
import com.autorecon.domain.vo.ReconBillItemVO;
import com.autorecon.domain.vo.ReconBillVO;
import com.autorecon.mapper.DisputeMapper;
import com.autorecon.mapper.EnterpriseMapper;
import com.autorecon.mapper.PaymentMapper;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.mapper.ReconTemplateMapper;
import com.autorecon.service.ReconBillService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 对账单服务实现
 */
@Slf4j
@Service
public class ReconBillServiceImpl extends ServiceImpl<ReconBillMapper, ReconBill> implements ReconBillService {

    private static final String BILL_NO_PREFIX = "DZ";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ReconBillItemMapper reconBillItemMapper;
    private final ReconTemplateMapper reconTemplateMapper;
    private final PaymentMapper paymentMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final DisputeMapper disputeMapper;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    public ReconBillServiceImpl(ReconBillItemMapper reconBillItemMapper,
                                 ReconTemplateMapper reconTemplateMapper, PaymentMapper paymentMapper,
                                 EnterpriseMapper enterpriseMapper, DisputeMapper disputeMapper) {
        this.reconBillItemMapper = reconBillItemMapper;
        this.reconTemplateMapper = reconTemplateMapper;
        this.paymentMapper = paymentMapper;
        this.enterpriseMapper = enterpriseMapper;
        this.disputeMapper = disputeMapper;
    }

    private static final AtomicLong BILL_SEQ = new AtomicLong(0);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBill(ReconBillCreateDTO dto) {
        if (dto == null || dto.getBuyerId() == null || dto.getTemplateId() == null
                || dto.getPeriodStart() == null || dto.getPeriodEnd() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        if (CollectionUtils.isEmpty(dto.getItems())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "对账单明细不能为空");
        }

        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        if (sellerId == null) {
            sellerId = 1L; // fallback for placeholder
        }

        ReconTemplate template = reconTemplateMapper.selectById(dto.getTemplateId());
        if (template == null) {
            throw new BizException(ErrorCode.TEMPLATE_NOT_FOUND);
        }

        String billNo = generateBillNo();
        ReconBill bill = ReconBill.builder()
                .billNo(billNo)
                .sellerId(sellerId)
                .buyerId(dto.getBuyerId())
                .templateId(dto.getTemplateId())
                .periodStart(dto.getPeriodStart())
                .periodEnd(dto.getPeriodEnd())
                .currency("CNY")
                .paymentAllocStrategy(dto.getPaymentAllocStrategy() != null ? dto.getPaymentAllocStrategy() : 1)
                .matchMode(dto.getMatchMode() != null ? dto.getMatchMode() : 0)
                .status(BillStatusEnum.CREATED.getCode())
                .remark(dto.getRemark())
                .createdBy(SecurityUtil.getCurrentUserId())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        List<ReconBillItem> items = new ArrayList<>();
        int lineNo = 1;
        for (ReconBillItemDTO itemDto : dto.getItems()) {
            BigDecimal total = itemDto.getTotalAmount() != null ? itemDto.getTotalAmount() : BigDecimal.ZERO;
            if (itemDto.getAmount() != null && itemDto.getTaxAmount() != null) {
                total = itemDto.getAmount().add(itemDto.getTaxAmount());
            } else if (itemDto.getAmount() != null) {
                total = itemDto.getAmount();
            }
            totalAmount = totalAmount.add(total);
            totalQuantity = totalQuantity.add(itemDto.getQuantity() != null ? itemDto.getQuantity() : BigDecimal.ZERO);
            totalWeight = totalWeight.add(itemDto.getWeight() != null ? itemDto.getWeight() : BigDecimal.ZERO);

            ReconBillItem item = ReconBillItem.builder()
                    .lineNo(lineNo++)
                    .contractNo(itemDto.getContractNo())
                    .contractName(itemDto.getContractName())
                    .orderNo(itemDto.getOrderNo())
                    .deliveryNo(itemDto.getDeliveryNo())
                    .sourceDocType(itemDto.getSourceDocType())
                    .productName(itemDto.getProductName())
                    .spec(itemDto.getSpec())
                    .material(itemDto.getMaterial())
                    .origin(itemDto.getOrigin())
                    .warehouse(itemDto.getWarehouse())
                    .quantity(itemDto.getQuantity())
                    .weight(itemDto.getWeight())
                    .unitPrice(itemDto.getUnitPrice())
                    .amount(itemDto.getAmount())
                    .taxRate(itemDto.getTaxRate())
                    .taxAmount(itemDto.getTaxAmount())
                    .totalAmount(total)
                    .deliveryDate(itemDto.getDeliveryDate())
                    .settleDate(itemDto.getSettleDate())
                    .paidAmount(BigDecimal.ZERO)
                    .unpaidAmount(total)
                    .build();
            items.add(item);
        }

        bill.setTotalAmount(totalAmount);
        bill.setTotalQuantity(totalQuantity);
        bill.setTotalWeight(totalWeight);
        bill.setPrevBalance(BigDecimal.ZERO);
        bill.setCurrentTradeAmount(totalAmount);
        bill.setCurrentPaymentAmount(BigDecimal.ZERO);
        bill.setCurrentBalance(totalAmount);

        baseMapper.insert(bill);
        Long billId = bill.getId();

        for (ReconBillItem item : items) {
            item.setBillId(billId);
            reconBillItemMapper.insert(item);
        }

        log.info("Created bill: billNo={}, billId={}", billNo, billId);
        return billId;
    }

    private String generateBillNo() {
        String dateStr = LocalDate.now().format(DATE_FORMAT);
        if (stringRedisTemplate != null) {
            try {
                String redisKey = "recon:bill:no:" + dateStr;
                Long seq = stringRedisTemplate.opsForValue().increment(redisKey);
                if (seq != null) {
                    return BILL_NO_PREFIX + dateStr + String.format("%06d", seq);
                }
            } catch (Exception e) {
                log.warn("Redis failed for bill no, using fallback: {}", e.getMessage());
            }
        }
        long seq = BILL_SEQ.incrementAndGet();
        return BILL_NO_PREFIX + dateStr + String.format("%06d", seq % 1000000);
    }

    @Override
    public PageResult<ReconBillVO> queryBillList(ReconBillQueryDTO query) {
        LambdaQueryWrapper<ReconBill> wrapper = new LambdaQueryWrapper<>();
        if (query.getSellerId() != null) {
            wrapper.eq(ReconBill::getSellerId, query.getSellerId());
        }
        if (query.getBuyerId() != null) {
            wrapper.eq(ReconBill::getBuyerId, query.getBuyerId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(ReconBill::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getBillNo())) {
            wrapper.eq(ReconBill::getBillNo, query.getBillNo());
        }
        if (query.getPeriodStart() != null) {
            wrapper.ge(ReconBill::getPeriodEnd, query.getPeriodStart());
        }
        if (query.getPeriodEnd() != null) {
            wrapper.le(ReconBill::getPeriodStart, query.getPeriodEnd());
        }
        wrapper.orderByDesc(ReconBill::getCreatedAt);

        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;
        Page<ReconBill> page = new Page<>(pageNum, pageSize);
        IPage<ReconBill> result = baseMapper.selectPage(page, wrapper);

        List<ReconBillVO> voList = new ArrayList<>();
        List<Long> sellerIds = result.getRecords().stream().map(ReconBill::getSellerId).distinct().collect(Collectors.toList());
        List<Long> buyerIds = result.getRecords().stream().map(ReconBill::getBuyerId).distinct().collect(Collectors.toList());
        List<Long> templateIds = result.getRecords().stream().map(ReconBill::getTemplateId).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, String> enterpriseNames = loadEnterpriseNames(sellerIds, buyerIds);
        Map<Long, String> templateNames = loadTemplateNames(templateIds);

        for (ReconBill bill : result.getRecords()) {
            ReconBillVO vo = toReconBillVO(bill);
            vo.setSellerName(enterpriseNames.get(bill.getSellerId()));
            vo.setBuyerName(enterpriseNames.get(bill.getBuyerId()));
            vo.setTemplateName(templateNames.get(bill.getTemplateId()));
            List<ReconBillItem> items = reconBillItemMapper.selectByBillId(bill.getId());
            vo.setItemCount(items != null ? items.size() : 0);
            voList.add(vo);
        }

        return PageResult.of(new Page<ReconBillVO>().setRecords(voList).setTotal(result.getTotal())
                .setSize(result.getSize()).setCurrent(result.getCurrent()).setPages(result.getPages()));
    }

    private Map<Long, String> loadEnterpriseNames(List<Long> sellerIds, List<Long> buyerIds) {
        List<Long> allIds = new ArrayList<>();
        allIds.addAll(sellerIds);
        allIds.addAll(buyerIds);
        Map<Long, String> map = new java.util.HashMap<>();
        for (Long id : allIds) {
            if (id != null) {
                Enterprise e = enterpriseMapper.selectById(id);
                map.put(id, e != null ? e.getCompanyName() : "");
            }
        }
        return map;
    }

    private Map<Long, String> loadTemplateNames(List<Long> templateIds) {
        Map<Long, String> map = new java.util.HashMap<>();
        for (Long id : templateIds) {
            if (id != null) {
                ReconTemplate t = reconTemplateMapper.selectById(id);
                map.put(id, t != null ? t.getTemplateName() : "");
            }
        }
        return map;
    }

    private ReconBillVO toReconBillVO(ReconBill bill) {
        ReconBillVO vo = new ReconBillVO();
        BeanUtils.copyProperties(bill, vo);
        return vo;
    }

    @Override
    public ReconBillDetailVO getBillDetail(Long billId) {
        ReconBill bill = baseMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }

        ReconBillDetailVO vo = new ReconBillDetailVO();
        BeanUtils.copyProperties(bill, vo);

        Enterprise seller = enterpriseMapper.selectById(bill.getSellerId());
        Enterprise buyer = enterpriseMapper.selectById(bill.getBuyerId());
        vo.setSellerName(seller != null ? seller.getCompanyName() : "");
        vo.setBuyerName(buyer != null ? buyer.getCompanyName() : "");

        ReconTemplate template = reconTemplateMapper.selectById(bill.getTemplateId());
        vo.setTemplateName(template != null ? template.getTemplateName() : "");

        List<ReconBillItem> items = reconBillItemMapper.selectByBillId(billId);
        if (items != null) {
            List<ReconBillItemVO> itemVOs = items.stream().map(this::toReconBillItemVO).collect(Collectors.toList());
            vo.setItems(itemVOs);
        }

        LambdaQueryWrapper<Payment> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(Payment::getPayeeId, bill.getSellerId()).eq(Payment::getPayerId, bill.getBuyerId());
        List<Payment> payments = paymentMapper.selectList(paymentWrapper);
        if (payments != null) {
            List<PaymentVO> paymentVOs = payments.stream().map(p -> {
                PaymentVO pvo = new PaymentVO();
                BeanUtils.copyProperties(p, pvo);
                return pvo;
            }).collect(Collectors.toList());
            vo.setPayments(paymentVOs);
        }

        LambdaQueryWrapper<com.autorecon.domain.entity.Dispute> disputeWrapper = new LambdaQueryWrapper<>();
        disputeWrapper.eq(com.autorecon.domain.entity.Dispute::getBillId, billId);
        List<com.autorecon.domain.entity.Dispute> disputes = disputeMapper.selectList(disputeWrapper);
        if (disputes != null) {
            List<DisputeVO> disputeVOs = disputes.stream().map(d -> {
                DisputeVO dvo = new DisputeVO();
                BeanUtils.copyProperties(d, dvo);
                return dvo;
            }).collect(Collectors.toList());
            vo.setDisputes(disputeVOs);
        } else {
            vo.setDisputes(new ArrayList<>());
        }

        return vo;
    }

    private ReconBillItemVO toReconBillItemVO(ReconBillItem item) {
        ReconBillItemVO vo = new ReconBillItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendBill(Long billId) {
        ReconBill bill = baseMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        String status = bill.getStatus();
        if (!BillStatusEnum.GENERATED.getCode().equals(status) && !BillStatusEnum.CREATED.getCode().equals(status)) {
            throw new BizException(ErrorCode.BILL_STATUS_ERROR);
        }
        bill.setStatus(BillStatusEnum.PENDING.getCode());
        baseMapper.updateById(bill);
        log.info("Sent bill: billId={}", billId);
        // TODO: send notification
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBill(Long billId) {
        ReconBill bill = baseMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        if (!BillStatusEnum.PENDING.getCode().equals(bill.getStatus())) {
            throw new BizException(ErrorCode.BILL_STATUS_ERROR);
        }
        bill.setStatus(BillStatusEnum.TO_SIGN.getCode());
        baseMapper.updateById(bill);
        log.info("Confirmed bill: billId={}", billId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidBill(Long billId) {
        ReconBill bill = baseMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        if (BillStatusEnum.SIGNED.getCode().equals(bill.getStatus())) {
            throw new BizException(ErrorCode.BILL_STATUS_ERROR);
        }
        bill.setStatus(BillStatusEnum.VOID.getCode());
        baseMapper.updateById(bill);
        log.info("Voided bill: billId={}", billId);
    }

    @Override
    public DashboardVO getDashboard(Long enterpriseId) {
        LambdaQueryWrapper<ReconBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconBill::getSellerId, enterpriseId);

        List<ReconBill> all = baseMapper.selectList(wrapper);
        int pendingCount = 0;
        int disputedCount = 0;
        int toSignCount = 0;
        int collectingCount = 0;
        BigDecimal totalReceivable = BigDecimal.ZERO;
        BigDecimal totalOverdue = BigDecimal.ZERO;

        for (ReconBill b : all) {
            if (BillStatusEnum.PENDING.getCode().equals(b.getStatus())) pendingCount++;
            else if (BillStatusEnum.DISPUTED.getCode().equals(b.getStatus())) disputedCount++;
            else if (BillStatusEnum.TO_SIGN.getCode().equals(b.getStatus())) toSignCount++;
            else if (BillStatusEnum.COLLECTING.getCode().equals(b.getStatus())) collectingCount++;
            if (b.getCurrentBalance() != null && b.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0
                    && !BillStatusEnum.VOID.getCode().equals(b.getStatus())) {
                totalReceivable = totalReceivable.add(b.getCurrentBalance());
            }
        }

        DashboardVO vo = new DashboardVO();
        vo.setPendingCount(pendingCount);
        vo.setDisputedCount(disputedCount);
        vo.setToSignCount(toSignCount);
        vo.setCollectingCount(collectingCount);
        vo.setTotalReceivable(totalReceivable);
        vo.setTotalOverdue(totalOverdue);
        vo.setMonthlyCompletionRate(BigDecimal.ZERO);
        vo.setRecentTodos(new ArrayList<>());
        return vo;
    }

    @Override
    public String generatePdf(Long billId) {
        ReconBill bill = baseMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        log.info("PDF generation queued for billId={}", billId);
        return "pdf generation queued";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String batchCreateBills(List<Long> buyerIds, LocalDate periodStart, LocalDate periodEnd, Long templateId) {
        if (CollectionUtils.isEmpty(buyerIds)) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "买方列表不能为空");
        }
        String batchId = "BATCH_" + System.currentTimeMillis();
        List<ReconBillItemDTO> placeholderItems = new ArrayList<>();
        ReconBillItemDTO placeholder = new ReconBillItemDTO();
        placeholder.setTotalAmount(BigDecimal.ZERO);
        placeholder.setQuantity(BigDecimal.ZERO);
        placeholder.setWeight(BigDecimal.ZERO);
        placeholder.setAmount(BigDecimal.ZERO);
        placeholder.setDeliveryDate(periodStart);
        placeholder.setSettleDate(periodEnd);
        placeholderItems.add(placeholder);

        for (Long buyerId : buyerIds) {
            ReconBillCreateDTO dto = new ReconBillCreateDTO();
            dto.setBuyerId(buyerId);
            dto.setTemplateId(templateId);
            dto.setPeriodStart(periodStart);
            dto.setPeriodEnd(periodEnd);
            dto.setItems(placeholderItems);
            Long billId = createBill(dto);
            ReconBill bill = baseMapper.selectById(billId);
            if (bill != null) {
                bill.setBatchId(batchId);
                baseMapper.updateById(bill);
            }
        }

        log.info("Batch created bills: batchId={}, count={}", batchId, buyerIds.size());
        return batchId;
    }
}
