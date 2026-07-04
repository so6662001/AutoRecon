package com.autorecon.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 异议类型枚举
 */
@Getter
@AllArgsConstructor
public enum DisputeTypeEnum {

    QUANTITY(1, "数量"),
    WEIGHT(2, "重量"),
    PRICE(3, "价格"),
    SPEC(4, "规格"),
    MISSING(5, "缺失"),
    OTHER(6, "其他");

    private final int value;
    private final String desc;

    public static DisputeTypeEnum of(int value) {
        for (DisputeTypeEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
