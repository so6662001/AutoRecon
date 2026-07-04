package com.autorecon.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 印章类型枚举
 */
@Getter
@AllArgsConstructor
public enum SealTypeEnum {

    OFFICIAL(1, "公章"),
    CONTRACT(2, "合同章"),
    FINANCE(3, "财务章"),
    LEGAL_PERSON(4, "法人章");

    private final int value;
    private final String desc;

    public static SealTypeEnum of(int value) {
        for (SealTypeEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
