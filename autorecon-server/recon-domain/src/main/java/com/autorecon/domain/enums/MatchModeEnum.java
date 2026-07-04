package com.autorecon.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 匹配模式枚举
 */
@Getter
@AllArgsConstructor
public enum MatchModeEnum {

    MANUAL(1, "手动"),
    ERP_AUTO(2, "ERP自动"),
    EXCEL_UPLOAD(3, "Excel上传"),
    ONLINE_FILL(4, "在线填写"),
    OCR(5, "OCR"),
    MOBILE(6, "移动端");

    private final int value;
    private final String desc;

    public static MatchModeEnum of(int value) {
        for (MatchModeEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
