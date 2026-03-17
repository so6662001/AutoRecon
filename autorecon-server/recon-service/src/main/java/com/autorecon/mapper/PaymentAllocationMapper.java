package com.autorecon.mapper;

import com.autorecon.domain.entity.PaymentAllocation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 付款抵扣明细 Mapper
 */
@Mapper
public interface PaymentAllocationMapper extends BaseMapper<PaymentAllocation> {
}
