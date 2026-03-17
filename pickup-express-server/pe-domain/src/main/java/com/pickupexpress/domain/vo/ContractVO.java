package com.pickupexpress.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 合同 VO
 */
@Data
public class ContractVO {

    private Long id;
    private String contractNo;
    private String erpContractId;
    private Integer contractType;
    private Long sellerId;
    private Long buyerId;
    private String buyerContactName;
    private String buyerContactPhone;
    private BigDecimal totalQuantity;
    private BigDecimal totalWeight;
    private BigDecimal totalAmount;
    private BigDecimal pickedWeight;
    private BigDecimal pickedAmount;
    private BigDecimal settledAmount;
    private BigDecimal paidAmount;
    private String paymentTerms;
    private LocalDate deliveryDeadline;
    private Long warehouseId;
    private String warehouseName;
    private Integer signRequired;
    private Integer signStatus;
    private String signedPdfUrl;
    private String signEvidenceNo;
    private Integer pickupMode;
    private Integer allowNoCodePickup;
    private Integer status;
    private LocalDateTime erpSyncAt;
    private Long templateId;
    private Integer templateVersion;
    private String platformTermsVersion;
    private String customClauses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String buyerName;
    private String sellerName;
    private Integer itemCount;
    private Integer pickupCount;
}
