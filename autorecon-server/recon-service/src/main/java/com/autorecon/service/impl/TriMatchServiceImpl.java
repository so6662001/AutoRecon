package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.domain.entity.InvoiceLink;
import com.autorecon.domain.entity.PaymentAllocation;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.vo.TriMatchVO;
import com.autorecon.mapper.InvoiceLinkMapper;
import com.autorecon.mapper.PaymentAllocationMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.TriMatchService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 三方匹配服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TriMatchServiceImpl implements TriMatchService {

    private final ReconBillMapper reconBillMapper;
    private final InvoiceLinkMapper invoiceLinkMapper;
    private final PaymentAllocationMapper paymentAllocationMapper;

    @Override
    public TriMatchVO getTriMatch(Long billId) {
        ReconBill bill = reconBillMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }

        BigDecimal totalAmount = bill.getTotalAmount() != null ? bill.getTotalAmount() : BigDecimal.ZERO;

        LambdaQueryWrapper<InvoiceLink> invoiceWrapper = new LambdaQueryWrapper<>();
        invoiceWrapper.eq(InvoiceLink::getBillId, billId);
        List<InvoiceLink> invoiceLinks = invoiceLinkMapper.selectList(invoiceWrapper);
        BigDecimal invoicedAmount = invoiceLinks.stream()
                .map(il -> il.getLinkAmount() != null ? il.getLinkAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal uninvoicedAmount = totalAmount.subtract(invoicedAmount).max(BigDecimal.ZERO);

        LambdaQueryWrapper<PaymentAllocation> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(PaymentAllocation::getBillId, billId);
        List<PaymentAllocation> allocations = paymentAllocationMapper.selectList(paymentWrapper);
        BigDecimal paidAmount = allocations.stream()
                .map(pa -> pa.getAllocatedAmount() != null ? pa.getAllocatedAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unpaidAmount = totalAmount.subtract(paidAmount).max(BigDecimal.ZERO);

        BigDecimal invoiceRate = totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? invoicedAmount.divide(totalAmount, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal paymentRate = totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? paidAmount.divide(totalAmount, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        TriMatchVO vo = new TriMatchVO();
        vo.setBillId(billId);
        vo.setBillNo(bill.getBillNo());
        vo.setTotalAmount(totalAmount);
        vo.setInvoicedAmount(invoicedAmount);
        vo.setUninvoicedAmount(uninvoicedAmount);
        vo.setPaidAmount(paidAmount);
        vo.setUnpaidAmount(unpaidAmount);
        vo.setInvoiceRate(invoiceRate);
        vo.setPaymentRate(paymentRate);
        vo.setInvoiceLinks(invoiceLinks);
        return vo;
    }
}
