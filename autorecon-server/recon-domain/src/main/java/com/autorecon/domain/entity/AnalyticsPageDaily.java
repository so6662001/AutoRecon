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
@TableName("analytics_page_daily")
public class AnalyticsPageDaily {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate statDate;

    @TableField("`system`")
    private String system;

    private String pagePath;
    private String pageName;
    private String pageModule;
    private Integer pv;
    private Integer uv;
    private Integer sessions;
    private Integer avgDuration;
    private Integer bounceCount;
    private Short avgFcp;
    private Short avgLcp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
