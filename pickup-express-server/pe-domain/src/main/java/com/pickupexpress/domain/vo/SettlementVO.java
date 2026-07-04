package com.pickupexpress.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算单 VO
 */
@Data
public class SettlementVO {

    private Long id;
    private String settlementNo;
    private Long pickupOrderId;
    private Long contractId;
    private String contractNo;
    private Long buyerId;
    private BigDecimal totalWeight;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalWithTax;
    private BigDecimal deductedPrepayment;
    private BigDecimal receivableAmount;
    private String settlementDetail;
    private String pdfUrl;
    private Integer customerViewed;
    private LocalDateTime customerViewedAt;
    private Integer syncedToRecon;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String buyerName;
}
