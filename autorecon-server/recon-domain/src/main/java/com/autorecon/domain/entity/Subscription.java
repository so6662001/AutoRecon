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
 * 企业订阅
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("subscription")
public class Subscription {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long enterpriseId;
    private Integer planType;
    private Integer billingCycle;
    private BigDecimal unitPrice;
    private BigDecimal discountRate;
    private BigDecimal actualPrice;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer autoRenew;
    private Integer status;
    private LocalDate trialEndDate;
    private String referralCode;
    private Long referredBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
