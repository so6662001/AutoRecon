package com.autorecon.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 访客对账单 VO
 */
@Data
public class GuestBillVO {

    private String billNo;
    private String sellerName;
    private String buyerName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalAmount;
    private BigDecimal currentPaymentAmount;
    private BigDecimal currentBalance;
    private Integer itemCount;
    private String pdfUrl;
    private Boolean confirmed;
    private Boolean expired;
}
