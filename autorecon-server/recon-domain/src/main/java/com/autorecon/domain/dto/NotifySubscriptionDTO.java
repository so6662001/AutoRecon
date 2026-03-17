package com.autorecon.domain.dto;

import lombok.Data;

/**
 * 通知订阅配置 DTO
 */
@Data
public class NotifySubscriptionDTO {

    private String eventType;
    private Integer channelSms;
    private Integer channelEmail;
    private Integer channelWechat;
    private Integer channelApp;
    private String quietStart;
    private String quietEnd;
    private Integer frequencyLimit;
}
