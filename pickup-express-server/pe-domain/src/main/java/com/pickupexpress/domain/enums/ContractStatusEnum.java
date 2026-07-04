package com.pickupexpress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合同状态枚举
 */
@Getter
@AllArgsConstructor
public enum ContractStatusEnum {

    READY(0, "可提货"),
    PENDING_SIGN(1, "待签约"),
    SIGNED(2, "已签约"),
    PICKING(3, "提货中"),
    PICKED(4, "已提完"),
    SETTLED(5, "已结清"),
    CLOSED(6, "已关闭"),
    OVERDUE(7, "超期预警");

    private final int value;
    private final String desc;

    public static ContractStatusEnum of(int value) {
        for (ContractStatusEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
