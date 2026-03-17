package com.pickupexpress.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pickupexpress.domain.dto.ConfirmTimeoutConfigDTO;
import com.pickupexpress.domain.entity.ConfirmTimeoutConfig;

/**
 * 确认时效配置服务
 */
public interface ConfirmTimeoutService extends IService<ConfirmTimeoutConfig> {

    ConfirmTimeoutConfig getConfig(Long enterpriseId, Long buyerId, String scenario);

    void saveConfig(ConfirmTimeoutConfigDTO dto);

    int getTimeoutHours(Long enterpriseId, Long buyerId, String scenario);
}
