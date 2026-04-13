package com.autorecon.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("analytics_action_daily")
public class AnalyticsActionDaily {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate statDate;

    @TableField("`system`")
    private String system;

    private String eventName;
    private String actionCategory;
    private Integer actionCount;
    private Integer actionUsers;
    private Integer actionEnterprises;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
