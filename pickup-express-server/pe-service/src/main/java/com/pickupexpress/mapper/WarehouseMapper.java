package com.pickupexpress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickupexpress.domain.entity.Warehouse;
import org.apache.ibatis.annotations.Mapper;

/**
 * 仓库 Mapper
 */
@Mapper
public interface WarehouseMapper extends BaseMapper<Warehouse> {
}
