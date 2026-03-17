package com.pickupexpress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据来源枚举
 */
@Getter
@AllArgsConstructor
public enum DataSourceEnum {

    WMS(1, "WMS"),
    H5(2, "H5"),
    THIRD_PARTY(3, "第三方WMS"),
    DRIVER(4, "驾驶员"),
    SUPPLEMENT(5, "事后补录");

    private final int value;
    private final String desc;

    public static DataSourceEnum of(int value) {
        for (DataSourceEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
