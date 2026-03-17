package com.pickupexpress.service.impl;

import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.NotificationLog;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.entity.SettlementOrder;
import com.pickupexpress.domain.enums.NotificationChannelEnum;
import com.pickupexpress.mapper.ContractMapper;
import com.pickupexpress.mapper.PickupOrderMapper;
import com.pickupexpress.mapper.SettlementOrderMapper;
import com.pickupexpress.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class NotificationServiceImpl implements NotificationService {

    private final com.pickupexpress.mapper.NotificationLogMapper notificationLogMapper;
    private final PickupOrderMapper pickupOrderMapper;
    private final ContractMapper contractMapper;
    private final SettlementOrderMapper settlementOrderMapper;

    @Override
    public void sendPickupStartNotification(Long pickupOrderId) {
        PickupOrder order = pickupOrderMapper.selectById(pickupOrderId);
        if (order == null) return;

        String content = String.format("您的提货已开始，合同号：%s，请按时到达仓库提货。", order.getContractNo());
        logNotification(pickupOrderId, order.getContractId(), order.getBuyerId(),
                NotificationChannelEnum.SMS.getValue(), order.getDriverPhone(), content);
        log.info("Placeholder: send pickup start SMS to {}", order.getDriverPhone());
    }

    @Override
    public void sendPickupCompleteNotification(Long pickupOrderId, BigDecimal totalWeight, BigDecimal totalAmount) {
        PickupOrder order = pickupOrderMapper.selectById(pickupOrderId);
        if (order == null) return;

        Contract contract = contractMapper.selectById(order.getContractId());
        String productInfo = contract != null ? contract.getContractNo() : "品规";
        String content = String.format("您的提货已完成: %s %s吨, 结算金额¥%s。查看详情: [链接]", productInfo, totalWeight, totalAmount);
        logNotification(pickupOrderId, order.getContractId(), order.getBuyerId(),
                NotificationChannelEnum.SMS.getValue(), order.getDriverPhone(), content);
        log.info("Placeholder: send pickup complete SMS to {}", order.getDriverPhone());
    }

    @Override
    public void sendSettlementNotification(Long settlementId) {
        SettlementOrder settlement = settlementOrderMapper.selectById(settlementId);
        if (settlement == null) return;

        String content = String.format("结算单已生成，应收金额：¥%s，请及时查看。", settlement.getReceivableAmount());
        logNotification(settlement.getPickupOrderId(), settlement.getContractId(), settlement.getBuyerId(),
                NotificationChannelEnum.SMS.getValue(), null, content);
        log.info("Placeholder: send settlement notification for settlementId={}", settlementId);
    }

    @Override
    public void sendVerificationSms(Long pickupOrderId, String buyerPhone, String content) {
        PickupOrder order = pickupOrderMapper.selectById(pickupOrderId);
        String phone = buyerPhone != null ? buyerPhone : (order != null ? order.getDriverPhone() : null);
        logNotification(pickupOrderId, order != null ? order.getContractId() : null,
                order != null ? order.getBuyerId() : null,
                NotificationChannelEnum.SMS.getValue(), phone, content);
        log.info("Placeholder: send verification SMS to {}", phone);
    }

    @Override
    public void sendDispatchConfirmNotification(Long pickupOrderId) {
        PickupOrder order = pickupOrderMapper.selectById(pickupOrderId);
        if (order == null) return;

        String content = String.format("派车已确认，司机：%s，车牌：%s，请按时到达。", order.getDriverName(), order.getVehiclePlate());
        logNotification(pickupOrderId, order.getContractId(), order.getBuyerId(),
                NotificationChannelEnum.SMS.getValue(), order.getDriverPhone(), content);
        log.info("Placeholder: send dispatch confirm SMS to {}", order.getDriverPhone());
    }

    @Override
    public NotificationLog logNotification(Long pickupOrderId, Long contractId, Long buyerId, Integer channel, String phone, String content) {
        NotificationLog notificationLog = NotificationLog.builder()
                .targetType("pickup_order")
                .targetId(pickupOrderId)
                .recipient(phone)
                .channel(channel)
                .title("提货通知")
                .content(content)
                .sent(1)
                .sentAt(LocalDateTime.now())
                .build();
        notificationLogMapper.insert(notificationLog);
        return notificationLog;
    }
}
