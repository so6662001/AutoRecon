package com.pickupexpress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickupexpress.domain.entity.AuthorizedPickupPerson;
import org.apache.ibatis.annotations.Mapper;

/**
 * 授权提货人 Mapper
 */
@Mapper
public interface AuthorizedPickupPersonMapper extends BaseMapper<AuthorizedPickupPerson> {
}
