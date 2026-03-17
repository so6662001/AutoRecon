package com.autorecon.mapper;

import com.autorecon.domain.entity.Subscription;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订阅 Mapper
 */
@Mapper
public interface SubscriptionMapper extends BaseMapper<Subscription> {
}
