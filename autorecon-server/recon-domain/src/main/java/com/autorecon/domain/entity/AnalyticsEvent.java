package com.autorecon.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("analytics_event")
public class AnalyticsEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String eventId;
    private String eventType;
    private String eventName;
    private LocalDateTime eventTime;

    @TableField("`system`")
    private String system;

    private Long userId;
    private Long enterpriseId;
    private Integer roleType;
    private Integer isGuest;
    private String sessionId;
    private String deviceId;
    private String platform;
    private String os;
    private String browser;
    private Short screenWidth;
    private Short screenHeight;
    private Integer isMobile;
    private String pagePath;
    private String pageName;
    private String pageTitle;
    private String pageModule;
    private String referrer;
    private Integer duration;
    private String actionCategory;
    private String actionLabel;
    private String actionValue;
    private String actionExtra;
    private Short perfFcp;
    private Short perfLcp;
    private Short perfFid;
    private Float perfCls;
    private String ip;
    private LocalDateTime createdAt;
}
