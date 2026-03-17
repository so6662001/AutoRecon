package com.pickupexpress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickupexpress.domain.entity.Contract;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同 Mapper
 */
@Mapper
public interface ContractMapper extends BaseMapper<Contract> {
}
