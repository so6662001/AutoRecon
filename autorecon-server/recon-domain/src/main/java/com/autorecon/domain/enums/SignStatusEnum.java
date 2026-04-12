package com.autorecon.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 签章状态枚举
 */
@Getter
@AllArgsConstructor
public enum SignStatusEnum {

    PENDING(0, "待签章"),
    SIGNING(1, "签章中"),
    SIGNED(2, "已签章"),
    REFUSED(3, "已拒绝"),
    CANCELLED(4, "已撤销");

    private final int value;
    private final String desc;

    public static SignStatusEnum of(int value) {
        for (SignStatusEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
