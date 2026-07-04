package com.pickupexpress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickupexpress.domain.entity.ContractItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同明细 Mapper
 */
@Mapper
public interface ContractItemMapper extends BaseMapper<ContractItem> {
}
