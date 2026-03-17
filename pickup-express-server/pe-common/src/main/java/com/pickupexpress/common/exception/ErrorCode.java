package com.pickupexpress.common.exception;

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

    CONTRACT_NOT_FOUND(10001, "合同不存在"),
    CONTRACT_STATUS_ERROR(10002, "合同状态不允许此操作"),
    CONTRACT_NOT_SIGNABLE(10003, "合同不可签约"),

    PICKUP_ORDER_NOT_FOUND(11001, "提货单不存在"),
    PICKUP_CODE_INVALID(11002, "提货码无效"),
    PICKUP_CODE_EXPIRED(11003, "提货码已过期"),

    DISPATCH_NOT_FOUND(12001, "派车信息不存在"),
    DISPATCH_ALREADY_CONFIRMED(12002, "派车已确认"),

    DELIVERY_NOT_STARTED(13001, "发货未开始"),
    DELIVERY_ALREADY_COMPLETED(13002, "发货已完成"),

    DRIVER_NOT_FOUND(14001, "驾驶员不存在"),
    DRIVER_NOT_ASSIGNED(14002, "驾驶员未分配"),

    SETTLEMENT_NOT_FOUND(15001, "结算单不存在"),

    VERIFICATION_DENIED(16001, "确权被拒绝"),
    VERIFICATION_FAILED(16002, "确权失败"),

    SYSTEM_ERROR(99999, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
