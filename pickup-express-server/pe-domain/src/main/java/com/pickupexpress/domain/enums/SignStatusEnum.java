package com.pickupexpress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 签约状态枚举
 */
@Getter
@AllArgsConstructor
public enum SignStatusEnum {

    NOT_SIGNED(0, "未签"),
    BUYER_SIGNED(1, "客户已签"),
    BOTH_SIGNED(2, "双方已签"),
    REJECTED(3, "拒签");

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
