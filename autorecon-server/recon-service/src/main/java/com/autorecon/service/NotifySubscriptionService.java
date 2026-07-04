package com.autorecon.service;

import com.autorecon.domain.dto.NotifySubscriptionDTO;
import com.autorecon.domain.entity.NotifySubscription;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 通知订阅服务接口
 */
public interface NotifySubscriptionService extends IService<NotifySubscription> {

    List<NotifySubscription> getSubscriptions(Long userId);

    void updateSubscriptions(Long userId, List<NotifySubscriptionDTO> subscriptions);

    void createDefaultSubscriptions(Long userId, Long enterpriseId);
}
