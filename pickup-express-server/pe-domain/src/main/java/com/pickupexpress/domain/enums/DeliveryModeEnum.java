package com.pickupexpress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 发货模式枚举
 */
@Getter
@AllArgsConstructor
public enum DeliveryModeEnum {

    WMS(1, "自有WMS"),
    H5_ASSISTANT(2, "H5发货助手"),
    THIRD_PARTY_WMS(3, "第三方WMS"),
    DRIVER_CONFIRM(4, "驾驶员确认"),
    SUPPLEMENT(5, "事后补录");

    private final int value;
    private final String desc;

    public static DeliveryModeEnum of(int value) {
        for (DeliveryModeEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
