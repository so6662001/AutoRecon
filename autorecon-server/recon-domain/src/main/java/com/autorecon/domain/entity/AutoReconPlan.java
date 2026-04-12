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

import java.time.LocalDateTime;

/**
 * 定期自动对账计划
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("auto_recon_plan")
public class AutoReconPlan {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long sellerId;
    private Long buyerId;
    private String planName;
    private Integer frequency;
    private Integer executionDay;
    /** DB TIME column; persisted as "HH:mm" string (e.g. "08:00"). */
    private String executionTime;
    private Long templateId;
    private Integer periodType;
    private Integer autoSend;
    private Integer includePayment;
    private Integer status;
    private String cronExpression;
    private LocalDateTime lastExecutedAt;
    private LocalDateTime nextExecuteAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
