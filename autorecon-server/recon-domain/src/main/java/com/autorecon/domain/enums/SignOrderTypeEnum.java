package com.autorecon.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 签章顺序类型枚举
 */
@Getter
@AllArgsConstructor
public enum SignOrderTypeEnum {

    SELLER_FIRST(1, "卖方先签"),
    BUYER_FIRST(2, "买方先签"),
    UNORDERED(3, "无序");

    private final int value;
    private final String desc;

    public static SignOrderTypeEnum of(int value) {
        for (SignOrderTypeEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
