package com.autorecon.domain.vo;

import com.autorecon.domain.entity.InvoiceLink;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 三方匹配 VO
 */
@Data
public class TriMatchVO {

    private Long billId;
    private String billNo;
    private BigDecimal totalAmount;
    private BigDecimal invoicedAmount;
    private BigDecimal uninvoicedAmount;
    private BigDecimal paidAmount;
    private BigDecimal unpaidAmount;
    private BigDecimal invoiceRate;        // 账票匹配率 = 已开票/总额
    private BigDecimal invoicePaymentRate; // 票款匹配率 = 已收款/已开票
    private BigDecimal paymentRate;        // 账款匹配率 = 已收款/总额
    private List<InvoiceLink> invoiceLinks;
}
