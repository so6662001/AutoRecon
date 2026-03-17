package com.autorecon.mapper;

import com.autorecon.domain.entity.AutoReconPlan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定期自动对账计划 Mapper
 */
@Mapper
public interface AutoReconPlanMapper extends BaseMapper<AutoReconPlan> {
}
