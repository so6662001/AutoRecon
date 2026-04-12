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
 * Per-enterprise UI settings (timeout rules, templates, recon rules).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("enterprise_settings")
public class EnterpriseSettings {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long enterpriseId;

    private Integer noDiffDays;
    private Integer specChangeHours;
    private Integer overDiffHours;
    private Integer settleDays;
    private Integer reminderHours;

    /** JSON array: customer-specific timeout overrides */
    private String timeoutCustomersJson;

    /** JSON array: notification templates */
    private String notificationTemplatesJson;

    /** JSON object: recon matching rules */
    private String reconRulesJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
