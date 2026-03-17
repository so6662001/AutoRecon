package com.pickupexpress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 确权强度枚举
 */
@Getter
@AllArgsConstructor
public enum VerificationLevelEnum {

    LIGHT(1, "轻度"),
    STANDARD(2, "标准"),
    ENHANCED(3, "加强"),
    STRICT(4, "严格"),
    STRICTEST(5, "最严格");

    private final int value;
    private final String desc;

    public static VerificationLevelEnum of(int value) {
        for (VerificationLevelEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
