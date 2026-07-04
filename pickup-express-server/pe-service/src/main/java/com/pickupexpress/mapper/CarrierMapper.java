package com.pickupexpress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickupexpress.domain.entity.Carrier;
import org.apache.ibatis.annotations.Mapper;

/**
 * 承运公司 Mapper
 */
@Mapper
public interface CarrierMapper extends BaseMapper<Carrier> {
}
