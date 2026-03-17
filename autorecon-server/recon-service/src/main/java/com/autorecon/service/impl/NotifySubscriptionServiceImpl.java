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

    private static final List<String> DEFAULT_EVENT_TYPES = Arrays.asList(
            "new_bill", "bill_expire", "dispute_reply", "signed", "payment_due"
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
        for (String eventType : DEFAULT_EVENT_TYPES) {
            NotifySubscription sub = NotifySubscription.builder()
                    .userId(userId)
                    .enterpriseId(enterpriseId)
                    .eventType(eventType)
                    .channelSms(0)
                    .channelEmail(0)
                    .channelWechat(0)
                    .channelApp(1)
                    .build();
            notifySubscriptionMapper.insert(sub);
        }
        log.info("Created default subscriptions for userId={}, enterpriseId={}", userId, enterpriseId);
    }
}
