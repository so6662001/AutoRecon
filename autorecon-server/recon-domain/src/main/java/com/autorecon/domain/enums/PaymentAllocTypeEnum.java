package com.autorecon.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 付款分配类型枚举
 */
@Getter
@AllArgsConstructor
public enum PaymentAllocTypeEnum {

    FIFO(1, "先进先出"),
    MANUAL(2, "手动分配"),
    PROPORTIONAL(3, "按比例分配");

    private final int value;
    private final String desc;

    public static PaymentAllocTypeEnum of(int value) {
        for (PaymentAllocTypeEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
