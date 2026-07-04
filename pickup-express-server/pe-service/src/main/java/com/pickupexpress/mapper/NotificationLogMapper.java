package com.pickupexpress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickupexpress.domain.entity.NotificationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知日志 Mapper
 */
@Mapper
public interface NotificationLogMapper extends BaseMapper<NotificationLog> {
}
