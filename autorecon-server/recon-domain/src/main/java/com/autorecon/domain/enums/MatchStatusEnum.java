package com.autorecon.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 匹配状态枚举
 */
@Getter
@AllArgsConstructor
public enum MatchStatusEnum {

    NOT_MATCHED(0, "未匹配"),
    MATCHED(1, "已匹配"),
    DIFF(2, "有差异"),
    SELLER_EXTRA(3, "卖方多余"),
    BUYER_EXTRA(4, "买方多余");

    private final int value;
    private final String desc;

    public static MatchStatusEnum of(int value) {
        for (MatchStatusEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
