package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickupexpress.common.util.SecurityUtil;
import com.pickupexpress.common.util.TenantUtil;
import com.pickupexpress.domain.dto.ConfirmTimeoutConfigDTO;
import com.pickupexpress.domain.entity.ConfirmTimeoutConfig;
import com.pickupexpress.mapper.ConfirmTimeoutConfigMapper;
import com.pickupexpress.service.ConfirmTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 确认时效配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ConfirmTimeoutServiceImpl extends ServiceImpl<ConfirmTimeoutConfigMapper, ConfirmTimeoutConfig>
        implements ConfirmTimeoutService {

    private static final int DEFAULT_HOURS = 24;

    @Override
    public ConfirmTimeoutConfig getConfig(Long enterpriseId, Long buyerId, String scenario) {
        ConfirmTimeoutConfig config = getOne(new LambdaQueryWrapper<ConfirmTimeoutConfig>()
                .eq(ConfirmTimeoutConfig::getEnterpriseId, enterpriseId)
                .eq(ConfirmTimeoutConfig::getBuyerId, buyerId)
                .eq(ConfirmTimeoutConfig::getConfigType, scenario)
                .last("LIMIT 1"));

        if (config != null) return config;

        config = getOne(new LambdaQueryWrapper<ConfirmTimeoutConfig>()
                .eq(ConfirmTimeoutConfig::getEnterpriseId, enterpriseId)
                .isNull(ConfirmTimeoutConfig::getBuyerId)
                .eq(ConfirmTimeoutConfig::getConfigType, scenario)
                .last("LIMIT 1"));

        if (config != null) return config;

        return ConfirmTimeoutConfig.builder()
                .enterpriseId(enterpriseId)
                .buyerId(buyerId)
                .configType(scenario)
                .hours(DEFAULT_HOURS)
                .build();
    }

    @Override
    public void saveConfig(ConfirmTimeoutConfigDTO dto) {
        TenantUtil.checkOwnership(dto.getEnterpriseId());

        ConfirmTimeoutConfig config;
        if (dto.getId() != null) {
            config = getById(dto.getId());
            if (config == null) return;
        } else {
            config = new ConfirmTimeoutConfig();
        }

        config.setEnterpriseId(dto.getEnterpriseId());
        config.setBuyerId(dto.getBuyerId());
        config.setContractType(dto.getContractType());
        config.setConfigType(dto.getConfigType());
        config.setHours(dto.getHours());
        config.setDays(dto.getDays());

        saveOrUpdate(config);
    }

    @Override
    public int getTimeoutHours(Long enterpriseId, Long buyerId, String scenario) {
        ConfirmTimeoutConfig config = getConfig(enterpriseId, buyerId, scenario);
        if (config.getHours() != null && config.getHours() > 0) {
            return config.getHours();
        }
        if (config.getDays() != null && config.getDays() > 0) {
            return config.getDays() * 24;
        }
        return DEFAULT_HOURS;
    }
}
