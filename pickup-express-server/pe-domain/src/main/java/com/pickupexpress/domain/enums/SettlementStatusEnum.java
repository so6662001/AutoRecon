package com.pickupexpress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 结算状态枚举
 */
@Getter
@AllArgsConstructor
public enum SettlementStatusEnum {

    NOT_SETTLED(0, "未结算"),
    SETTLED(1, "已结算");

    private final int value;
    private final String desc;

    public static SettlementStatusEnum of(int value) {
        for (SettlementStatusEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
