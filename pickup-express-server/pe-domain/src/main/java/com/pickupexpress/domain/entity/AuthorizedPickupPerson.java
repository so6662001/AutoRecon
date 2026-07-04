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
 * 授权提货人
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("authorized_pickup_person")
public class AuthorizedPickupPerson {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long buyerId;
    private Long contractId;
    private String personName;
    private String idCardNo;
    private String phone;
    private String vehiclePlate;
    private BigDecimal maxPickupWeight;
    private Integer registeredBy;
    private Integer buyerConfirmed;
    private LocalDateTime buyerConfirmedAt;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
