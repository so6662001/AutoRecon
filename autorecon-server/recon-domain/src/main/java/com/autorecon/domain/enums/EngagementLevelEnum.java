package com.autorecon.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 买方参与等级枚举
 */
@Getter
@AllArgsConstructor
public enum EngagementLevelEnum {

    GUEST(0, "免注册"),
    LIGHT(1, "轻量注册"),
    DATA(2, "数据参与"),
    SEAL(3, "签章认证"),
    DEEP(4, "深度集成");

    private final int value;
    private final String desc;

    public static EngagementLevelEnum of(int value) {
        for (EngagementLevelEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
