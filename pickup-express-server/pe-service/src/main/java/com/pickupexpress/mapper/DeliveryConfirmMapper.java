package com.pickupexpress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickupexpress.domain.entity.DeliveryConfirm;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发货确认 Mapper
 */
@Mapper
public interface DeliveryConfirmMapper extends BaseMapper<DeliveryConfirm> {
}
