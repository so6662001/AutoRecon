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
import java.time.LocalDateTime;

/**
 * 提货单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("pickup_order")
public class PickupOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String pickupNo;
    private Long contractId;
    private String contractNo;
    private Long buyerId;
    private String pickupCode;
    private String pickupCodeQr;
    private Integer pickupCodeStatus;
    private LocalDateTime pickupCodeExpireAt;
    private Integer dispatchMode;
    private Integer dispatchStatus;
    private Integer customerConfirmed;
    private LocalDateTime customerConfirmedAt;
    private String vehiclePlate;
    private String driverName;
    private String driverPhone;
    private Long carrierId;
    private String carrierName;
    private Integer driverAssigned;
    private LocalDateTime driverAssignedAt;
    private Long warehouseId;
    private String warehouseName;
    private LocalDateTime expectedArrivalAt;
    private LocalDateTime actualArrivalAt;
    private BigDecimal arrivalGpsLat;
    private BigDecimal arrivalGpsLng;
    private Integer totalLifts;
    private Integer totalPieces;
    private BigDecimal totalWeight;
    private BigDecimal totalAmount;
    private Integer deliveryMode;
    private String deliveryDataSource;
    private Integer deliveryStatus;
    private Integer settlementStatus;
    private Long evidencePackageId;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
