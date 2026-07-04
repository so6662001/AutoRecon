package com.autorecon.service.impl;

import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.NotifySubscriptionDTO;
import com.autorecon.domain.entity.NotifySubscription;
import com.autorecon.mapper.NotifySubscriptionMapper;
import com.autorecon.service.NotifySubscriptionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 通知订阅服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class NotifySubscriptionServiceImpl extends ServiceImpl<NotifySubscriptionMapper, NotifySubscription> implements NotifySubscriptionService {

    /**
     * 设计文档8.12定义的11种事件类型
     */
    private static final List<String> DEFAULT_EVENT_TYPES = Arrays.asList(
            "new_bill",          // 📋 收到新对账单
            "bill_expire",       // ⏰ 对账单即将超时
            "dispute_reply",     // ⚡ 收到异议回复
            "signed",            // ✅ 对账单已签章
            "payment_due",       // 💰 付款到期提醒
            "collection",        // 📊 催收通知
            "monthly_report",    // 📈 月度对账报告
            "system_notice",     // 🔔 系统公告
            "credit_change",     // ⚠️ 信用评分变更
            "invoice_linked",    // 📑 发票关联完成
            "finance_status"     // 💳 保理融资状态变更
    );

    private final NotifySubscriptionMapper notifySubscriptionMapper;

    @Override
    public List<NotifySubscription> getSubscriptions(Long userId) {
        LambdaQueryWrapper<NotifySubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotifySubscription::getUserId, userId)
                .orderByAsc(NotifySubscription::getEventType);
        return notifySubscriptionMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubscriptions(Long userId, List<NotifySubscriptionDTO> subscriptions) {
        LambdaQueryWrapper<NotifySubscription> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(NotifySubscription::getUserId, userId);
        notifySubscriptionMapper.delete(deleteWrapper);

        if (!CollectionUtils.isEmpty(subscriptions)) {
            Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
            for (NotifySubscriptionDTO dto : subscriptions) {
                NotifySubscription sub = new NotifySubscription();
                BeanUtils.copyProperties(dto, sub);
                sub.setUserId(userId);
                sub.setEnterpriseId(enterpriseId);
                notifySubscriptionMapper.insert(sub);
            }
        }
        log.info("Updated subscriptions for userId={}, count={}", userId, subscriptions != null ? subscriptions.size() : 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDefaultSubscriptions(Long userId, Long enterpriseId) {
        // 先检查是否已有订阅(避免重复创建)
        Long existing = notifySubscriptionMapper.selectCount(
                new LambdaQueryWrapper<NotifySubscription>()
                        .eq(NotifySubscription::getUserId, userId)
                        .eq(NotifySubscription::getEnterpriseId, enterpriseId));
        if (existing != null && existing > 0) {
            log.info("Subscriptions already exist for userId={}, skipping", userId);
            return;
        }

        for (String eventType : DEFAULT_EVENT_TYPES) {
            NotifySubscription sub = NotifySubscription.builder()
                    .userId(userId)
                    .enterpriseId(enterpriseId)
                    .eventType(eventType)
                    .channelApp(1) // 站内信默认全开
                    .channelSms(getDefaultSms(eventType))
                    .channelEmail(getDefaultEmail(eventType))
                    .channelWechat(getDefaultWechat(eventType))
                    .frequencyLimit(getDefaultFrequency(eventType))
                    .build();
            notifySubscriptionMapper.insert(sub);
        }
        log.info("Created default subscriptions for userId={}, enterpriseId={}, count={}",
                userId, enterpriseId, DEFAULT_EVENT_TYPES.size());
    }

    /**
     * 查询用户对特定事件的订阅配置(供通知分发使用)
     */
    public NotifySubscription getSubscription(Long userId, String eventType) {
        return notifySubscriptionMapper.selectOne(
                new LambdaQueryWrapper<NotifySubscription>()
                        .eq(NotifySubscription::getUserId, userId)
                        .eq(NotifySubscription::getEventType, eventType));
    }

    /**
     * 判断是否应发送通知(考虑免打扰时段)
     */
    public boolean shouldSend(NotifySubscription sub, String channel) {
        if (sub == null) return false;
        // 检查渠道是否开启
        boolean channelEnabled = switch (channel) {
            case "app" -> sub.getChannelApp() != null && sub.getChannelApp() == 1;
            case "sms" -> sub.getChannelSms() != null && sub.getChannelSms() == 1;
            case "email" -> sub.getChannelEmail() != null && sub.getChannelEmail() == 1;
            case "wechat" -> sub.getChannelWechat() != null && sub.getChannelWechat() == 1;
            default -> false;
        };
        if (!channelEnabled) return false;

        // 检查免打扰时段
        if (sub.getQuietStart() != null && sub.getQuietEnd() != null) {
            try {
                java.time.LocalTime now = java.time.LocalTime.now();
                java.time.LocalTime quietStart = java.time.LocalTime.parse(sub.getQuietStart());
                java.time.LocalTime quietEnd = java.time.LocalTime.parse(sub.getQuietEnd());
                if (quietStart.isBefore(quietEnd)) {
                    // 正常区间 e.g. 22:00~08:00 → 不在此范围内
                    // 实际上22:00~08:00是跨午夜的
                    if (now.isAfter(quietStart) || now.isBefore(quietEnd)) {
                        log.debug("In quiet period for user {}, delaying notification", sub.getUserId());
                        return false;
                    }
                } else {
                    // 跨午夜 e.g. 22:00~08:00
                    if (now.isAfter(quietStart) || now.isBefore(quietEnd)) {
                        return false;
                    }
                }
            } catch (Exception e) {
                // 解析失败忽略免打扰
            }
        }
        return true;
    }

    // ===== 默认渠道配置(按设计文档8.12表格) =====

    private int getDefaultSms(String eventType) {
        return switch (eventType) {
            case "new_bill", "bill_expire", "payment_due", "collection",
                 "credit_change", "finance_status" -> 1;
            default -> 0;
        };
    }

    private int getDefaultEmail(String eventType) {
        return switch (eventType) {
            case "new_bill", "signed", "payment_due",
                 "monthly_report", "invoice_linked", "finance_status" -> 1;
            default -> 0;
        };
    }

    private int getDefaultWechat(String eventType) {
        return switch (eventType) {
            case "new_bill", "bill_expire", "dispute_reply",
                 "payment_due", "finance_status" -> 1;
            default -> 0;
        };
    }

    private int getDefaultFrequency(String eventType) {
        return switch (eventType) {
            case "monthly_report" -> 3;   // 每月汇总
            case "invoice_linked" -> 2;   // 每日汇总
            default -> 1;                 // 每次
        };
    }
}
