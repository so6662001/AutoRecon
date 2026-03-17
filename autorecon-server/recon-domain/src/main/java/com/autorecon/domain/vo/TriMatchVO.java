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
    private BigDecimal invoiceRate;
    private BigDecimal paymentRate;
    private List<InvoiceLink> invoiceLinks;
}
