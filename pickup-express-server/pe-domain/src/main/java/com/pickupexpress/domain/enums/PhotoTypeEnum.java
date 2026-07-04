package com.pickupexpress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 照片类型枚举
 */
@Getter
@AllArgsConstructor
public enum PhotoTypeEnum {

    LOADING(1, "装载全景"),
    GOODS(2, "货物近景"),
    PLATE(3, "车牌"),
    SCALE(4, "磅单"),
    OTHER(5, "其他");

    private final int value;
    private final String desc;

    public static PhotoTypeEnum of(int value) {
        for (PhotoTypeEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
