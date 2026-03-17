package com.pickupexpress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 提货单状态枚举
 */
@Getter
@AllArgsConstructor
public enum PickupOrderStatusEnum {

    DISPATCH_PENDING(1, "待派车"),
    READY(2, "待提货"),
    ACCEPTED(3, "已接单"),
    ARRIVED(4, "已到达"),
    DELIVERING(5, "发货中"),
    COMPLETED(6, "已完成"),
    CANCELLED(7, "已取消");

    private final int value;
    private final String desc;

    public static PickupOrderStatusEnum of(int value) {
        for (PickupOrderStatusEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
