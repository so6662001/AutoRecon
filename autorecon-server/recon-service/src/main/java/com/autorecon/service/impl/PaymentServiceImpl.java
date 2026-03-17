package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.common.util.TenantUtil;
import com.autorecon.domain.dto.PaymentAllocateDTO;
import com.autorecon.domain.dto.PaymentCreateDTO;
import com.autorecon.domain.entity.Payment;
import com.autorecon.domain.entity.PaymentAllocation;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.enums.PaymentAllocTypeEnum;
import com.autorecon.mapper.PaymentAllocationMapper;
import com.autorecon.mapper.PaymentMapper;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 付款服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

    private static final String PAYMENT_NO_PREFIX = "PAY";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong PAYMENT_SEQ = new AtomicLong(0);

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private final PaymentMapper paymentMapper;
    private final PaymentAllocationMapper paymentAllocationMapper;
    private final ReconBillMapper reconBillMapper;
    private final ReconBillItemMapper reconBillItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPayment(PaymentCreateDTO dto) {
        if (dto.getPaymentAmount() == null || dto.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.PAYMENT_AMOUNT_ERROR);
        }

        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        Long payeeId = currentEnterpriseId != null ? currentEnterpriseId : dto.getPayeeId();

        String paymentNo = generatePaymentNo();
        Payment payment = Payment.builder()
                .paymentNo(paymentNo)
                .payerId(dto.getPayerId())
                .payeeId(payeeId)
                .paymentDate(dto.getPaymentDate())
                .paymentAmount(dto.getPaymentAmount())
                .paymentMethod(dto.getPaymentMethod())
                .bankSerialNo(dto.getBankSerialNo())
                .allocatedAmount(BigDecimal.ZERO)
                .unallocatedAmount(dto.getPaymentAmount())
                .source(1)
                .status(1)
                .remark(dto.getRemark())
                .build();

        paymentMapper.insert(payment);
        log.info("Created payment: id={}, paymentNo={}", payment.getId(), paymentNo);
        return payment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void allocatePayment(PaymentAllocateDTO dto) {
        Payment payment = paymentMapper.selectById(dto.getPaymentId());
        if (payment == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "付款记录不存在");
        }
        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId != null) {
            if (!currentEnterpriseId.equals(payment.getPayerId()) && !currentEnterpriseId.equals(payment.getPayeeId())) {
                throw new BizException(ErrorCode.FORBIDDEN);
            }
        }
        if (payment.getUnallocatedAmount() == null || payment.getUnallocatedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.PAYMENT_ALREADY_ALLOCATED);
        }

        BigDecimal totalAllocate = dto.getAllocations().stream()
                .map(PaymentAllocateDTO.AllocationItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAllocate.compareTo(payment.getUnallocatedAmount()) > 0) {
            throw new BizException(ErrorCode.PAYMENT_AMOUNT_ERROR.getCode(), "分配金额超过未分配金额");
        }

        for (PaymentAllocateDTO.AllocationItem item : dto.getAllocations()) {
            if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "分配金额必须为正数");
            }
        }

        for (PaymentAllocateDTO.AllocationItem item : dto.getAllocations()) {
            ReconBillItem billItem = item.getBillItemId() != null
                    ? reconBillItemMapper.selectById(item.getBillItemId())
                    : null;
            if (billItem == null) {
                continue;
            }
            ReconBill bill = reconBillMapper.selectById(item.getBillId());
            if (bill == null || !bill.getId().equals(billItem.getBillId())) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "对账单明细与对账单不匹配");
            }
            if (!bill.getSellerId().equals(payment.getPayeeId()) || !bill.getBuyerId().equals(payment.getPayerId())) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "对账单的买卖方与付款不匹配");
            }

            BigDecimal unpaid = billItem.getUnpaidAmount() != null ? billItem.getUnpaidAmount() : BigDecimal.ZERO;
            BigDecimal allocateAmount = item.getAmount().min(unpaid);

            PaymentAllocation allocation = PaymentAllocation.builder()
                    .paymentId(dto.getPaymentId())
                    .billId(item.getBillId())
                    .billItemId(item.getBillItemId())
                    .sourceDocNo(item.getSourceDocNo())
                    .allocatedAmount(allocateAmount)
                    .allocationType(PaymentAllocTypeEnum.MANUAL.getValue())
                    .allocatedAt(LocalDateTime.now())
                    .build();
            paymentAllocationMapper.insert(allocation);

            billItem.setPaidAmount((billItem.getPaidAmount() != null ? billItem.getPaidAmount() : BigDecimal.ZERO).add(allocateAmount));
            billItem.setUnpaidAmount(unpaid.subtract(allocateAmount));
            reconBillItemMapper.updateById(billItem);
        }

        payment.setAllocatedAmount((payment.getAllocatedAmount() != null ? payment.getAllocatedAmount() : BigDecimal.ZERO).add(totalAllocate));
        payment.setUnallocatedAmount(payment.getUnallocatedAmount().subtract(totalAllocate));
        paymentMapper.updateById(payment);
        log.info("Allocated payment: paymentId={}", dto.getPaymentId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoAllocateFIFO(Long paymentId) {
        Payment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "付款记录不存在");
        }
        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId != null) {
            if (!currentEnterpriseId.equals(payment.getPayerId()) && !currentEnterpriseId.equals(payment.getPayeeId())) {
                throw new BizException(ErrorCode.FORBIDDEN);
            }
        }
        BigDecimal remaining = payment.getUnallocatedAmount();
        if (remaining == null || remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.PAYMENT_ALREADY_ALLOCATED);
        }

        LambdaQueryWrapper<ReconBill> billWrapper = new LambdaQueryWrapper<>();
        billWrapper.eq(ReconBill::getSellerId, payment.getPayeeId())
                .eq(ReconBill::getBuyerId, payment.getPayerId())
                .ne(ReconBill::getStatus, "VOID");
        List<ReconBill> bills = reconBillMapper.selectList(billWrapper);
        List<Long> billIds = bills.stream().map(ReconBill::getId).toList();
        if (billIds.isEmpty()) {
            log.info("No bills for auto-allocate, paymentId={}", paymentId);
            return;
        }

        LambdaQueryWrapper<ReconBillItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(ReconBillItem::getBillId, billIds)
                .gt(ReconBillItem::getUnpaidAmount, 0)
                .orderByAsc(ReconBillItem::getDeliveryDate);

        List<ReconBillItem> items = reconBillItemMapper.selectList(itemWrapper);
        if (CollectionUtils.isEmpty(items)) {
            log.info("No unpaid items for auto-allocate, paymentId={}", paymentId);
            return;
        }

        BigDecimal totalAllocated = BigDecimal.ZERO;
        for (ReconBillItem item : items) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal unpaid = item.getUnpaidAmount() != null ? item.getUnpaidAmount() : BigDecimal.ZERO;
            if (unpaid.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal allocateAmount = unpaid.min(remaining);

            PaymentAllocation allocation = PaymentAllocation.builder()
                    .paymentId(paymentId)
                    .billId(item.getBillId())
                    .billItemId(item.getId())
                    .sourceDocNo(item.getDeliveryNo())
                    .allocatedAmount(allocateAmount)
                    .allocationType(PaymentAllocTypeEnum.FIFO.getValue())
                    .allocatedAt(LocalDateTime.now())
                    .build();
            paymentAllocationMapper.insert(allocation);

            item.setPaidAmount((item.getPaidAmount() != null ? item.getPaidAmount() : BigDecimal.ZERO).add(allocateAmount));
            item.setUnpaidAmount(unpaid.subtract(allocateAmount));
            reconBillItemMapper.updateById(item);

            totalAllocated = totalAllocated.add(allocateAmount);
            remaining = remaining.subtract(allocateAmount);
        }

        payment.setAllocatedAmount((payment.getAllocatedAmount() != null ? payment.getAllocatedAmount() : BigDecimal.ZERO).add(totalAllocated));
        payment.setUnallocatedAmount(payment.getUnallocatedAmount().subtract(totalAllocated));
        paymentMapper.updateById(payment);
        log.info("Auto-allocated FIFO: paymentId={}, amount={}", paymentId, totalAllocated);
    }

    @Override
    public BigDecimal getBalance(Long sellerId, Long buyerId) {
        LambdaQueryWrapper<ReconBill> billWrapper = new LambdaQueryWrapper<>();
        billWrapper.eq(ReconBill::getSellerId, sellerId)
                .eq(ReconBill::getBuyerId, buyerId)
                .ne(ReconBill::getStatus, "VOID");
        List<ReconBill> bills = reconBillMapper.selectList(billWrapper);
        return bills.stream()
                .map(ReconBill::getCurrentBalance)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<Payment> listPayments(Long payerId, Long payeeId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId != null) {
            wrapper.and(w -> w.eq(Payment::getPayerId, currentEnterpriseId).or().eq(Payment::getPayeeId, currentEnterpriseId));
        }
        if (payerId != null) wrapper.eq(Payment::getPayerId, payerId);
        if (payeeId != null) wrapper.eq(Payment::getPayeeId, payeeId);
        if (start != null) wrapper.ge(Payment::getPaymentDate, start);
        if (end != null) wrapper.le(Payment::getPaymentDate, end);
        wrapper.orderByDesc(Payment::getPaymentDate);
        return paymentMapper.selectList(wrapper);
    }

    private String generatePaymentNo() {
        String dateStr = LocalDate.now().format(DATE_FORMAT);
        if (stringRedisTemplate != null) {
            try {
                String redisKey = "recon:payment:no:" + dateStr;
                Long seq = stringRedisTemplate.opsForValue().increment(redisKey);
                if (seq != null) {
                    return PAYMENT_NO_PREFIX + dateStr + String.format("%06d", seq);
                }
            } catch (Exception e) {
                log.warn("Redis failed for payment no, using fallback: {}", e.getMessage());
            }
        }
        long seq = PAYMENT_SEQ.incrementAndGet();
        return PAYMENT_NO_PREFIX + dateStr + String.format("%06d", seq % 1000000);
    }
}
