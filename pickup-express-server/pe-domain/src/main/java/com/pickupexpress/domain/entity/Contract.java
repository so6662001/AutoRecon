package com.pickupexpress.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售合同
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("contract")
public class Contract {

    @TableId(type = IdType.ASSIGN_ID)
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
