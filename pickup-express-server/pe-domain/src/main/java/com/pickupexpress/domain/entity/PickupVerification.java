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

import java.time.LocalDateTime;

/**
 * 提货确权记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("pickup_verification")
public class PickupVerification {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long pickupOrderId;
    private Long contractId;
    private Long buyerId;
    private String driverName;
    private String driverPhone;
    private String vehiclePlate;
    private Integer isPreRegistered;
    private Long authorizedPersonId;
    private Integer smsSent;
    private LocalDateTime smsSentAt;
    private Integer smsDelivered;
    private String smsDeliveryReceipt;
    private Integer smsLinkClicked;
    private LocalDateTime smsLinkClickedAt;
    private Integer phoneCallMade;
    private Integer phoneCallResult;
    private String phoneCallRecordingUrl;
    private LocalDateTime phoneCallAt;
    private String driverIdPhotoUrl;
    private String driverFacePhotoUrl;
    private String driverLicensePhotoUrl;
    private Integer verificationLevel;
    private Integer verificationResult;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
