package com.pickupexpress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 派车模式枚举
 */
@Getter
@AllArgsConstructor
public enum DispatchModeEnum {

    CUSTOMER(1, "客户派车"),
    SALES(2, "销售派车"),
    CARRIER(3, "承运公司");

    private final int value;
    private final String desc;

    public static DispatchModeEnum of(int value) {
        for (DispatchModeEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
