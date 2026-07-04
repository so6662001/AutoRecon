package com.autorecon.service;

import com.autorecon.domain.dto.BuyerDataConfigDTO;
import com.autorecon.domain.entity.BuyerDataConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 买方数据配置服务接口
 */
public interface BuyerConfigService extends IService<BuyerDataConfig> {

    BuyerDataConfig getConfig(Long enterpriseId);

    void saveConfig(BuyerDataConfigDTO dto);
}
