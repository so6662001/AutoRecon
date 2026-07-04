package com.pickupexpress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickupexpress.domain.entity.AnalyticsEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnalyticsEventMapper extends BaseMapper<AnalyticsEvent> {
}
