package com.pickupexpress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 进度事件类型枚举
 */
@Getter
@AllArgsConstructor
public enum EventTypeEnum {

    CONTRACT_SYNCED("CONTRACT_SYNCED", "合同从ERP同步"),
    CONTRACT_SIGN_REQUESTED("CONTRACT_SIGN_REQUESTED", "合同发起签约"),
    CONTRACT_SIGNED("CONTRACT_SIGNED", "合同签署完成"),
    PICKUP_VERIFIED("PICKUP_VERIFIED", "提货确权完成"),
    PICKUP_VERIFICATION_DENIED("PICKUP_VERIFICATION_DENIED", "客户否认提货"),
    DISPATCH_REQUESTED("DISPATCH_REQUESTED", "派车申请"),
    DISPATCH_CONFIRMED("DISPATCH_CONFIRMED", "派车确认"),
    DISPATCH_REJECTED("DISPATCH_REJECTED", "派车拒绝"),
    PICKUP_ORDER_CREATED("PICKUP_ORDER_CREATED", "提货单生成"),
    PICKUP_CODE_SENT("PICKUP_CODE_SENT", "提货码发送"),
    DRIVER_ASSIGNED("DRIVER_ASSIGNED", "驾驶员分配"),
    DRIVER_ACCEPTED("DRIVER_ACCEPTED", "驾驶员接单"),
    DRIVER_ARRIVED("DRIVER_ARRIVED", "驾驶员到达"),
    PICKUP_CODE_VERIFIED("PICKUP_CODE_VERIFIED", "提货码验证"),
    LIFT_UPLOADED("LIFT_UPLOADED", "吊装上传"),
    DELIVERY_COMPLETED("DELIVERY_COMPLETED", "发货完成"),
    DELIVERY_SIGNED("DELIVERY_SIGNED", "仓库签字确认"),
    SETTLEMENT_CREATED("SETTLEMENT_CREATED", "结算单生成"),
    SETTLEMENT_NOTIFIED("SETTLEMENT_NOTIFIED", "结算单通知客户"),
    SETTLEMENT_VIEWED("SETTLEMENT_VIEWED", "客户查看结算单"),
    EVIDENCE_ARCHIVED("EVIDENCE_ARCHIVED", "证据归档");

    private final String value;
    private final String desc;

    public static EventTypeEnum of(String value) {
        if (value == null) {
            return null;
        }
        for (EventTypeEnum e : values()) {
            if (e.getValue().equals(value)) {
                return e;
            }
        }
        return null;
    }
}
