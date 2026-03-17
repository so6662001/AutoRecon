package com.pickupexpress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickupexpress.domain.entity.PickupOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提货单 Mapper
 */
@Mapper
public interface PickupOrderMapper extends BaseMapper<PickupOrder> {
}
