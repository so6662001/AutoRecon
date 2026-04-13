package com.autorecon.common.exception;

import lombok.Getter;

/**
 * Error code enum for business exceptions.
 */
@Getter
public enum ErrorCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),

    BILL_NOT_FOUND(10001, "对账单不存在"),
    BILL_STATUS_ERROR(10002, "对账单状态不允许此操作"),
    BILL_ALREADY_SENT(10003, "对账单已发送，不可修改"),
    TEMPLATE_NOT_FOUND(10004, "模板不存在"),

    SIGN_SEAL_NOT_FOUND(20001, "印章不存在"),
    SIGN_NO_PERMISSION(20002, "没有签章权限"),
    SIGN_AMOUNT_EXCEED(20003, "超出签章金额上限，需审批"),
    SIGN_ENTERPRISE_NOT_AUTH(20004, "企业未完成实名认证"),

    PAYMENT_AMOUNT_ERROR(30001, "付款金额有误"),
    PAYMENT_ALREADY_ALLOCATED(30002, "付款已分配完毕"),

    GUEST_TOKEN_EXPIRED(40001, "链接已过期"),
    GUEST_TOKEN_INVALID(40002, "链接无效"),
    GUEST_PHONE_VERIFY_FAILED(40003, "手机验证失败"),

    QUOTA_EXCEEDED(50001, "套餐额度已用完"),
    SUBSCRIPTION_EXPIRED(50002, "订阅已过期"),

    AGREEMENT_VERSION_NOT_FOUND(60001, "协议版本不存在"),
    AGREEMENT_TYPE_MISMATCH(60002, "协议类型与版本不一致"),

    SYSTEM_ERROR(99999, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
