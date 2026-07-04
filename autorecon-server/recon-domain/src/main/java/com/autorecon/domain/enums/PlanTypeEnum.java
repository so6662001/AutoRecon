package com.autorecon.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 套餐类型枚举
 */
@Getter
@AllArgsConstructor
public enum PlanTypeEnum {

    FREE(0, "免费版"),
    BASIC(1, "基础版"),
    STANDARD(2, "标准版"),
    ENTERPRISE(3, "企业版");

    private final int value;
    private final String desc;

    public static PlanTypeEnum of(int value) {
        for (PlanTypeEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
