package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.BuyerDataConfigDTO;
import com.autorecon.domain.entity.BuyerDataConfig;
import com.autorecon.mapper.BuyerDataConfigMapper;
import com.autorecon.service.BuyerConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 买方数据配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuyerConfigServiceImpl extends ServiceImpl<BuyerDataConfigMapper, BuyerDataConfig> implements BuyerConfigService {

    private final BuyerDataConfigMapper buyerDataConfigMapper;

    @Override
    public BuyerDataConfig getConfig(Long enterpriseId) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        if (eid == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        LambdaQueryWrapper<BuyerDataConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuyerDataConfig::getEnterpriseId, eid).last("LIMIT 1");
        BuyerDataConfig config = buyerDataConfigMapper.selectOne(wrapper);
        if (config == null) {
            config = BuyerDataConfig.builder()
                    .enterpriseId(eid)
                    .submitMode(1)
                    .defaultConfirmMode(1)
                    .ocrEnabled(0)
                    .mobileEnabled(1)
                    .build();
            buyerDataConfigMapper.insert(config);
        }
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(BuyerDataConfigDTO dto) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        LambdaQueryWrapper<BuyerDataConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuyerDataConfig::getEnterpriseId, enterpriseId).last("LIMIT 1");
        BuyerDataConfig config = buyerDataConfigMapper.selectOne(wrapper);
        if (config == null) {
            config = BuyerDataConfig.builder()
                    .enterpriseId(enterpriseId)
                    .submitMode(dto.getSubmitMode() != null ? dto.getSubmitMode() : 1)
                    .excelMappingConfig(dto.getExcelMappingConfig())
                    .defaultConfirmMode(dto.getDefaultConfirmMode() != null ? dto.getDefaultConfirmMode() : 1)
                    .ocrEnabled(dto.getOcrEnabled() != null ? dto.getOcrEnabled() : 0)
                    .mobileEnabled(dto.getMobileEnabled() != null ? dto.getMobileEnabled() : 1)
                    .build();
            buyerDataConfigMapper.insert(config);
        } else {
            if (dto.getSubmitMode() != null) config.setSubmitMode(dto.getSubmitMode());
            if (dto.getExcelMappingConfig() != null) config.setExcelMappingConfig(dto.getExcelMappingConfig());
            if (dto.getDefaultConfirmMode() != null) config.setDefaultConfirmMode(dto.getDefaultConfirmMode());
            if (dto.getOcrEnabled() != null) config.setOcrEnabled(dto.getOcrEnabled());
            if (dto.getMobileEnabled() != null) config.setMobileEnabled(dto.getMobileEnabled());
            buyerDataConfigMapper.updateById(config);
        }
        log.info("Saved buyer config: enterpriseId={}", enterpriseId);
    }
}
