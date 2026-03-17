package com.autorecon.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账单明细 VO
 */
@Data
public class ReconBillItemVO {

    private Long id;
    private Long billId;
    private Integer lineNo;
    private String contractNo;
    private String contractName;
    private String orderNo;
    private String deliveryNo;
    private Integer sourceDocType;
    private String productName;
    private String spec;
    private String material;
    private String origin;
    private String warehouse;
    private BigDecimal quantity;
    private BigDecimal weight;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private LocalDate deliveryDate;
    private LocalDate settleDate;
    private Integer matchStatus;
    private BigDecimal buyerQuantity;
    private BigDecimal buyerWeight;
    private BigDecimal buyerAmount;
    private BigDecimal diffQuantity;
    private BigDecimal diffWeight;
    private BigDecimal diffAmount;
    private BigDecimal paidAmount;
    private BigDecimal unpaidAmount;
    private String invoiceNo;
    private Integer invoiceStatus;
    private Integer disputeRiskLevel;
    private String disputeRiskReason;
    private String weightDiffCause;
    private Integer buyerDataSource;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 扩展字段
    private String matchStatusDesc;
}
