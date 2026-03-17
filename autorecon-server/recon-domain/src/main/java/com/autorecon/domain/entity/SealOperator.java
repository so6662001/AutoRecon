package com.autorecon.domain.entity;

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
 * 签章经办人
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("seal_operator")
public class SealOperator {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long enterpriseId;
    private Long userId;
    private String operatorName;
    private String phone;
    private String idNo;
    private String thirdPartyPersonId;
    private Integer personalAuthStatus;
    private String allowedSealTypes;
    private BigDecimal amountLimit;
    private Integer requireApproval;
    private Long approvalUserId;
    private Integer verifyMethod;
    private LocalDate authExpireDate;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
