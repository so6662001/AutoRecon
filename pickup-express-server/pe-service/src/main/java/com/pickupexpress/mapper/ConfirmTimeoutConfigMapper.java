package com.pickupexpress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickupexpress.domain.entity.ConfirmTimeoutConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 确认时效配置 Mapper
 */
@Mapper
public interface ConfirmTimeoutConfigMapper extends BaseMapper<ConfirmTimeoutConfig> {
}
