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
 * 容差学习记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tolerance_learn")
public class ToleranceLearn {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long sellerId;
    private Long buyerId;
    private String dimension;
    private BigDecimal currentTolerance;
    private BigDecimal suggestedTolerance;
    private Integer sampleCount;
    private BigDecimal matchRateCurrent;
    private BigDecimal matchRateSuggested;
    private BigDecimal falsePositiveRate;
    private BigDecimal confidence;
    private Integer adopted;
    private LocalDateTime calculatedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
