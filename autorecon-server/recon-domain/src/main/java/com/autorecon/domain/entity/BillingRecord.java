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
import java.time.LocalDateTime;

/**
 * 账单记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("billing_record")
public class BillingRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long enterpriseId;
    private String billingMonth;
    private BigDecimal subscriptionFee;
    private BigDecimal sealFee;
    private BigDecimal collectionFee;
    private BigDecimal ocrFee;
    private BigDecimal financeFee;
    private BigDecimal otherFee;
    private BigDecimal totalFee;
    private BigDecimal discountAmount;
    private BigDecimal actualAmount;
    private Integer paymentStatus;
    private LocalDateTime paidAt;
    private Integer invoiceStatus;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
