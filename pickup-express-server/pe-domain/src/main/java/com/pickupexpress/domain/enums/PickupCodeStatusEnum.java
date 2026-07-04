package com.pickupexpress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 提货码状态枚举
 */
@Getter
@AllArgsConstructor
public enum PickupCodeStatusEnum {

    UNUSED(0, "未使用"),
    VERIFIED(1, "已验证"),
    USED(2, "已提货"),
    EXPIRED(3, "已过期");

    private final int value;
    private final String desc;

    public static PickupCodeStatusEnum of(int value) {
        for (PickupCodeStatusEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
