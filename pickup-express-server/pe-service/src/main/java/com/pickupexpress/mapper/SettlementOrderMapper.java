package com.pickupexpress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickupexpress.domain.entity.SettlementOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 结算单 Mapper
 */
@Mapper
public interface SettlementOrderMapper extends BaseMapper<SettlementOrder> {
}
