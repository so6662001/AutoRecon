package com.autorecon.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 付款方式枚举
 */
@Getter
@AllArgsConstructor
public enum PaymentMethodEnum {

    BANK_TRANSFER(1, "银行转账"),
    ACCEPTANCE_BILL(2, "承兑汇票"),
    CASH(3, "现金"),
    OTHER(4, "其他");

    private final int value;
    private final String desc;

    public static PaymentMethodEnum of(int value) {
        for (PaymentMethodEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
