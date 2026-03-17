package com.autorecon.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账单 VO
 */
@Data
public class ReconBillVO {

    private Long id;
    private String billNo;
    private String batchId;
    private Long sellerId;
    private Long buyerId;
    private Long templateId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalAmount;
    private BigDecimal totalQuantity;
    private BigDecimal totalWeight;
    private String currency;
    private BigDecimal prevBalance;
    private BigDecimal currentTradeAmount;
    private BigDecimal currentPaymentAmount;
    private BigDecimal currentBalance;
    private Integer paymentAllocStrategy;
    private String status;
    private Integer matchMode;
    private Integer matchResult;
    private BigDecimal disputePredictionScore;
    private Integer sellerSignStatus;
    private Integer buyerSignStatus;
    private LocalDateTime autoConfirmDeadline;
    private Integer autoConfirmed;
    private String pdfUrl;
    private String signedPdfUrl;
    private Integer sourceType;
    private Long autoReconPlanId;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 扩展字段
    private String sellerName;
    private String buyerName;
    private String templateName;
    private Integer itemCount;
}
