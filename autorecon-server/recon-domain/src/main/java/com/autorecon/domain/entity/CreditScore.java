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
 * 催收信用评分
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("credit_score")
public class CreditScore {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long enterpriseId;
    private Long sellerId;
    private BigDecimal creditScore;
    private String scoreLevel;
    private BigDecimal avgPaymentDays;
    private BigDecimal overdueRate;
    private BigDecimal disputeRate;
    private BigDecimal totalTradeAmount;
    private BigDecimal totalOverdueAmount;
    private String scoreFactors;
    private LocalDateTime lastCalculatedAt;
    private Integer trend;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
