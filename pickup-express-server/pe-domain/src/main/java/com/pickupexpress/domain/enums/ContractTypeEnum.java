package com.pickupexpress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合同类型枚举
 */
@Getter
@AllArgsConstructor
public enum ContractTypeEnum {

    RESERVED(1, "留货合同"),
    ORDER(2, "订货合同"),
    FRAMEWORK(3, "框架协议"),
    SIMPLE(4, "简易合同");

    private final int value;
    private final String desc;

    public static ContractTypeEnum of(int value) {
        for (ContractTypeEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
