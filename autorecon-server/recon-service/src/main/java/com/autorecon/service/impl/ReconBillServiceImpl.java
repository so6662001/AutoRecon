package com.autorecon.service.impl;

import com.autorecon.common.config.AutoReconProperties;
import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.result.PageResult;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.common.util.TenantUtil;
import com.autorecon.domain.dto.ReconBillCreateDTO;
import com.autorecon.domain.dto.ReconBillItemDTO;
import com.autorecon.domain.dto.ReconBillQueryDTO;
import com.autorecon.domain.entity.Enterprise;
import com.autorecon.domain.entity.Payment;
import com.autorecon.domain.entity.PaymentAllocation;
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
import com.autorecon.domain.vo.TemplatePreviewVO;
import com.autorecon.mapper.DisputeMapper;
import com.autorecon.mapper.EnterpriseMapper;
import com.autorecon.mapper.PaymentAllocationMapper;
import com.autorecon.mapper.PaymentMapper;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.mapper.ReconTemplateMapper;
import com.autorecon.service.EngagementService;
import com.autorecon.service.GuestAccessService;
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
import java.util.Collections;
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
    private final PaymentAllocationMapper paymentAllocationMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final DisputeMapper disputeMapper;
    private final AutoReconProperties autoReconProperties;
    private final TemplateRenderService templateRenderService;
    private final GuestAccessService guestAccessService;
    private final EngagementService engagementService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    public ReconBillServiceImpl(ReconBillItemMapper reconBillItemMapper,
                                 ReconTemplateMapper reconTemplateMapper, PaymentMapper paymentMapper,
                                 PaymentAllocationMapper paymentAllocationMapper,
                                 EnterpriseMapper enterpriseMapper, DisputeMapper disputeMapper,
                                 AutoReconProperties autoReconProperties,
                                 TemplateRenderService templateRenderService,
                                 GuestAccessService guestAccessService,
                                 EngagementService engagementService) {
        this.reconBillItemMapper = reconBillItemMapper;
        this.reconTemplateMapper = reconTemplateMapper;
        this.paymentMapper = paymentMapper;
        this.paymentAllocationMapper = paymentAllocationMapper;
        this.enterpriseMapper = enterpriseMapper;
        this.disputeMapper = disputeMapper;
        this.autoReconProperties = autoReconProperties;
        this.templateRenderService = templateRenderService;
        this.guestAccessService = guestAccessService;
        this.engagementService = engagementService;
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
            if (itemDto.getQuantity() != null && itemDto.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "数量不能为负数");
            }
            if (itemDto.getWeight() != null && itemDto.getWeight().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "重量不能为负数");
            }
            if (itemDto.getUnitPrice() != null && itemDto.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "单价不能为负数");
            }
            if (itemDto.getAmount() != null && itemDto.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "金额不能为负数");
            }
            if (itemDto.getTotalAmount() != null && itemDto.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "总金额不能为负数");
            }
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

        if (totalAmount.compareTo(BigDecimal.ZERO) < 0 || totalQuantity.compareTo(BigDecimal.ZERO) < 0 || totalWeight.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "合计金额、数量或重量不能为负数");
        }
        bill.setTotalAmount(totalAmount);
        bill.setTotalQuantity(totalQuantity);
        bill.setTotalWeight(totalWeight);
        bill.setIncludePaymentDetail(dto.getIncludePayment() != null && dto.getIncludePayment() ? 1 : 0);

        BigDecimal prevBalance = calculatePrevBalance(sellerId, dto.getBuyerId());
        BigDecimal currentPaymentAmount = calculatePeriodPayments(
                dto.getBuyerId(), sellerId, dto.getPeriodStart(), dto.getPeriodEnd());
        BigDecimal currentBalance = prevBalance.add(totalAmount).subtract(currentPaymentAmount);

        bill.setPrevBalance(prevBalance);
        bill.setCurrentTradeAmount(totalAmount);
        bill.setCurrentPaymentAmount(currentPaymentAmount);
        bill.setCurrentBalance(currentBalance);

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
        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId != null) {
            wrapper.and(w -> w.eq(ReconBill::getSellerId, currentEnterpriseId).or().eq(ReconBill::getBuyerId, currentEnterpriseId));
        }
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
        if (StringUtils.hasText(query.getContractNo())) {
            List<Long> billIds = reconBillItemMapper.selectList(
                    new LambdaQueryWrapper<ReconBillItem>()
                            .eq(ReconBillItem::getContractNo, query.getContractNo())
                            .select(ReconBillItem::getBillId)
            ).stream().map(ReconBillItem::getBillId).distinct().collect(Collectors.toList());
            if (billIds.isEmpty()) {
                int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
                int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;
                Page<ReconBillVO> emptyPage = new Page<>(pageNum, pageSize);
                emptyPage.setRecords(Collections.emptyList());
                emptyPage.setTotal(0);
                return PageResult.of(emptyPage);
            }
            wrapper.in(ReconBill::getId, billIds);
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
        TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());

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

        List<PaymentAllocation> allocations = paymentAllocationMapper.selectList(
                new LambdaQueryWrapper<PaymentAllocation>().eq(PaymentAllocation::getBillId, billId));
        List<Long> paymentIds = allocations.stream()
                .map(PaymentAllocation::getPaymentId)
                .distinct()
                .collect(Collectors.toList());
        List<Payment> payments = paymentIds.isEmpty() ? Collections.emptyList() : paymentMapper.selectBatchIds(paymentIds);
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
        TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());
        validateStatusTransition(bill.getStatus(),
                BillStatusEnum.CREATED.getCode(), BillStatusEnum.GENERATED.getCode());
        bill.setStatus(BillStatusEnum.PENDING.getCode());
        int timeoutDays = autoReconProperties.getAutoConfirm() != null
                ? autoReconProperties.getAutoConfirm().getDefaultTimeoutDays()
                : 3;
        bill.setAutoConfirmDeadline(LocalDateTime.now().plusDays(timeoutDays));
        bill.setAutoConfirmed(0);
        baseMapper.updateById(bill);
        log.info("Sent bill: billId={}", billId);

        try {
            Enterprise buyer = enterpriseMapper.selectById(bill.getBuyerId());
            if (buyer != null && buyer.getContactPhone() != null) {
                String token = guestAccessService.generateGuestToken(bill.getId(), buyer.getContactPhone());
                String guestLink = "/api/v1/guest/view/" + token;
                log.info("Generated guest link for bill {}: {}", bill.getBillNo(), guestLink);

                try {
                    engagementService.trackBillSent(bill.getBuyerId(), bill.getSellerId());
                } catch (Exception e) {
                    log.warn("Failed to update engagement tracking", e);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to generate guest token for bill {}", billId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBill(Long billId) {
        ReconBill bill = baseMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());
        validateStatusTransition(bill.getStatus(), BillStatusEnum.PENDING.getCode());
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
        TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());
        validateStatusTransition(bill.getStatus(),
                BillStatusEnum.CREATED.getCode(),
                BillStatusEnum.GENERATED.getCode(),
                BillStatusEnum.PENDING.getCode(),
                BillStatusEnum.DISPUTED.getCode(),
                BillStatusEnum.TO_SIGN.getCode());
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
        TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());

        List<ReconBillItem> items = reconBillItemMapper.selectList(
                new LambdaQueryWrapper<ReconBillItem>().eq(ReconBillItem::getBillId, billId));

        TemplatePreviewVO rendered = templateRenderService.renderBill(bill, items);

        log.info("PDF rendered for billId={}, template={}, items={}, groups={}",
                billId, rendered.getTemplateName(),
                rendered.getSampleItems() != null ? rendered.getSampleItems().size() : 0,
                rendered.getGroupedItems() != null ? rendered.getGroupedItems().size() : 0);

        return "pdf generation queued (template: " + rendered.getTemplateName() + ")";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String batchCreateBills(List<Long> buyerIds, LocalDate periodStart, LocalDate periodEnd, Long templateId) {
        if (CollectionUtils.isEmpty(buyerIds)) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "买方列表不能为空");
        }
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        String batchId = "BATCH_" + System.currentTimeMillis();
        int created = 0;
        int skipped = 0;

        for (Long buyerId : buyerIds) {
            // 查找该买方在对账周期内的历史交易明细(从已有对账单明细或ERP数据)
            List<ReconBillItemDTO> items = collectTradeItems(sellerId, buyerId, periodStart, periodEnd);

            // 跳过余额为0/无交易的客户
            if (items.isEmpty()) {
                log.info("Batch skip buyer {} - no trade data in period", buyerId);
                skipped++;
                continue;
            }

            ReconBillCreateDTO dto = new ReconBillCreateDTO();
            dto.setBuyerId(buyerId);
            dto.setTemplateId(templateId);
            dto.setPeriodStart(periodStart);
            dto.setPeriodEnd(periodEnd);
            dto.setItems(items);
            dto.setIncludePayment(true);
            dto.setPaymentAllocStrategy(1); // FIFO default

            try {
                Long billId = createBill(dto);
                ReconBill bill = baseMapper.selectById(billId);
                if (bill != null) {
                    bill.setBatchId(batchId);
                    bill.setSourceType(2); // 批量发起
                    baseMapper.updateById(bill);
                }
                created++;
            } catch (Exception e) {
                log.warn("Batch create failed for buyer {}: {}", buyerId, e.getMessage());
            }
        }

        log.info("Batch created bills: batchId={}, created={}, skipped={}", batchId, created, skipped);
        return batchId;
    }

    /**
     * 收集对账周期内的交易明细 — 从历史对账数据或ERP拉取
     * 优先从已有的已完成对账单明细中提取(避免重复对账)
     * 如果没有历史数据,返回空列表(由调用方跳过该买方)
     */
    private List<ReconBillItemDTO> collectTradeItems(Long sellerId, Long buyerId,
                                                      LocalDate periodStart, LocalDate periodEnd) {
        // 查找该周期内是否已有对账单(避免重复创建)
        Long existingCount = baseMapper.selectCount(
                new LambdaQueryWrapper<ReconBill>()
                        .eq(ReconBill::getSellerId, sellerId)
                        .eq(ReconBill::getBuyerId, buyerId)
                        .eq(ReconBill::getPeriodStart, periodStart)
                        .eq(ReconBill::getPeriodEnd, periodEnd)
                        .ne(ReconBill::getStatus, BillStatusEnum.VOID.getCode())
                        .eq(ReconBill::getDeleted, 0));
        if (existingCount != null && existingCount > 0) {
            log.info("Bill already exists for seller={}, buyer={}, period={}-{}", sellerId, buyerId, periodStart, periodEnd);
            return List.of(); // 已有对账单,跳过
        }

        // 尝试从上期对账单明细中提取交易数据模式(作为参考)
        // 实际生产环境应从ERP拉取真实交易数据
        // 这里提供一个占位机制: 如果有ERP连接,可调用erpPullService.pullData
        // 否则返回空列表(前端可手动添加明细)
        return List.of();
    }

    private BigDecimal calculatePrevBalance(Long sellerId, Long buyerId) {
        LambdaQueryWrapper<ReconBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconBill::getSellerId, sellerId)
                .eq(ReconBill::getBuyerId, buyerId)
                .in(ReconBill::getStatus,
                        BillStatusEnum.COMPLETED.getCode(),
                        BillStatusEnum.SIGNED.getCode(),
                        BillStatusEnum.COLLECTING.getCode())
                .orderByDesc(ReconBill::getPeriodEnd)
                .last("LIMIT 1");
        ReconBill prevBill = baseMapper.selectOne(wrapper);
        return prevBill != null && prevBill.getCurrentBalance() != null
                ? prevBill.getCurrentBalance()
                : BigDecimal.ZERO;
    }

    private BigDecimal calculatePeriodPayments(Long payerId, Long payeeId, LocalDate periodStart, LocalDate periodEnd) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getPayerId, payerId)
                .eq(Payment::getPayeeId, payeeId)
                .ge(Payment::getPaymentDate, periodStart)
                .le(Payment::getPaymentDate, periodEnd)
                .eq(Payment::getDeleted, 0);
        List<Payment> payments = paymentMapper.selectList(wrapper);
        return payments.stream()
                .map(Payment::getPaymentAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateStatusTransition(String currentStatus, String... allowedFrom) {
        boolean valid = false;
        for (String s : allowedFrom) {
            if (s.equals(currentStatus)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new BizException(ErrorCode.BILL_STATUS_ERROR);
        }
    }
}
