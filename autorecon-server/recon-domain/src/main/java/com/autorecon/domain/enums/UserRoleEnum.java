package com.autorecon.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
@AllArgsConstructor
public enum UserRoleEnum {

    SELLER_ADMIN(1, "卖方管理员"),
    SELLER_OPERATOR(2, "卖方操作员"),
    SELLER_FINANCE(3, "卖方财务"),
    BUYER_ADMIN(4, "买方管理员"),
    BUYER_OPERATOR(5, "买方操作员"),
    PLATFORM_ADMIN(6, "平台管理员");

    private final int value;
    private final String desc;

    public static UserRoleEnum of(int value) {
        for (UserRoleEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
