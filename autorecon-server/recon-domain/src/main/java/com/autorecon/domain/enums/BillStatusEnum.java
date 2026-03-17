package com.autorecon.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账单状态枚举
 */
@Getter
@AllArgsConstructor
public enum BillStatusEnum {

    CREATED("CREATED", "已创建", 10),
    GENERATED("GENERATED", "已生成", 20),
    PENDING("PENDING", "待审核", 30),
    DISPUTED("DISPUTED", "异议中", 40),
    TO_SIGN("TO_SIGN", "待签章", 50),
    SIGNED("SIGNED", "已签章", 60),
    COLLECTING("COLLECTING", "催收中", 70),
    FINANCING("FINANCING", "融资中", 75),
    COMPLETED("COMPLETED", "已完成", 80),
    VOID("VOID", "已作废", 99);

    private final String code;
    private final String desc;
    private final int sort;

    public static BillStatusEnum of(String code) {
        for (BillStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
