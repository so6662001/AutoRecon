package com.pickupexpress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知渠道枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationChannelEnum {

    SMS(1, "短信"),
    WECHAT(2, "企微"),
    DINGTALK(3, "钉钉"),
    WEBSOCKET(4, "WebSocket");

    private final int value;
    private final String desc;

    public static NotificationChannelEnum of(int value) {
        for (NotificationChannelEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
