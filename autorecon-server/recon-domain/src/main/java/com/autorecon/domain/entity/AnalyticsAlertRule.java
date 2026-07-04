package com.autorecon.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("analytics_alert_rule")
public class AnalyticsAlertRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String alertName;
    private String metric;
    private String conditionType;
    private BigDecimal threshold;
    private Integer windowMinutes;
    private String notifyChannels;
    private Integer enabled;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
