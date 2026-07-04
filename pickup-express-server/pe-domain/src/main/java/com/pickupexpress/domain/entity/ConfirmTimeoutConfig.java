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
 * 确认时效配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("confirm_timeout_config")
public class ConfirmTimeoutConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long enterpriseId;
    private Long buyerId;
    @TableField("scenario")
    private String scenario;
    @TableField("timeout_value")
    private Integer timeoutValue;
    @TableField("timeout_unit")
    private String timeoutUnit;
    @TableField("reminder_before_hours")
    private Integer reminderBeforeHours;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

    /** Alias for scenario - used by service layer */
    public String getConfigType() {
        return scenario;
    }

    /** Hours when timeout_unit is 'hours' */
    public Integer getHours() {
        return "hours".equals(timeoutUnit) ? timeoutValue : null;
    }

    /** Days when timeout_unit is 'days' */
    public Integer getDays() {
        return "days".equals(timeoutUnit) ? timeoutValue : null;
    }
}
