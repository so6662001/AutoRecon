package com.pickupexpress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 发货状态枚举
 */
@Getter
@AllArgsConstructor
public enum DeliveryStatusEnum {

    NOT_STARTED(0, "未开始"),
    IN_PROGRESS(1, "发货中"),
    COMPLETED(2, "已完成");

    private final int value;
    private final String desc;

    public static DeliveryStatusEnum of(int value) {
        for (DeliveryStatusEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
