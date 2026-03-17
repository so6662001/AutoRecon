package com.pickupexpress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickupexpress.domain.entity.PickupVerification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提货确权记录 Mapper
 */
@Mapper
public interface PickupVerificationMapper extends BaseMapper<PickupVerification> {
}
