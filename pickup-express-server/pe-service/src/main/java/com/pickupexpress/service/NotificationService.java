package com.pickupexpress.service;

import com.pickupexpress.domain.entity.NotificationLog;

import java.math.BigDecimal;

/**
 * 通知服务
 */
public interface NotificationService {

    void sendPickupStartNotification(Long pickupOrderId);

    void sendPickupCompleteNotification(Long pickupOrderId, BigDecimal totalWeight, BigDecimal totalAmount);

    void sendSettlementNotification(Long settlementId);

    void sendVerificationSms(Long pickupOrderId, String buyerPhone, String content);

    void sendDispatchConfirmNotification(Long pickupOrderId);

    NotificationLog logNotification(Long pickupOrderId, Long contractId, Long buyerId, Integer channel, String phone, String content);
}
