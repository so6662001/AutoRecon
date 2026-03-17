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
 * 催收计划
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("collection_plan")
public class CollectionPlan {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long billId;
    private Long sellerId;
    private Long buyerId;
    private BigDecimal receivableAmount;
    private BigDecimal collectedAmount;
    private BigDecimal remainingAmount;
    private LocalDate dueDate;
    private String strategyLevel;
    private Integer status;
    private Integer currentStage;
    private LocalDate nextActionDate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
