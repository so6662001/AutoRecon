package com.pickupexpress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审核状态枚举
 */
@Getter
@AllArgsConstructor
public enum ApprovalStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    private final int value;
    private final String desc;

    public static ApprovalStatusEnum of(int value) {
        for (ApprovalStatusEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
