package com.autorecon.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 来源单据类型枚举
 */
@Getter
@AllArgsConstructor
public enum SourceDocTypeEnum {

    CONTRACT_TRADE(1, "合同制交易"),
    SPOT_TRADE(2, "现货/零单交易"),
    ADJUSTMENT(3, "补差结算"),
    RETURN(4, "退货");

    private final int value;
    private final String desc;

    public static SourceDocTypeEnum of(int value) {
        for (SourceDocTypeEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
