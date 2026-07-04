package com.pickupexpress.domain.vo;

import com.pickupexpress.domain.entity.ContractItem;
import com.pickupexpress.domain.entity.ProgressEvent;
import com.pickupexpress.domain.entity.SettlementOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合同详情 VO
 */
@Data
public class ContractDetailVO {

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

    private List<ContractItem> items;
    private List<PickupOrderVO> pickupOrders;
    private List<SettlementOrder> settlements;
    private List<ProgressEvent> progressEvents;
}
