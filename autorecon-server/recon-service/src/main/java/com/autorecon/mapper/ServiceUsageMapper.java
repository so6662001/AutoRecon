package com.autorecon.mapper;

import com.autorecon.domain.entity.ServiceUsage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务使用量 Mapper
 */
@Mapper
public interface ServiceUsageMapper extends BaseMapper<ServiceUsage> {
}
