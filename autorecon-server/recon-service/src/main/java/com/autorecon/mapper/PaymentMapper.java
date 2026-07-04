package com.autorecon.mapper;

import com.autorecon.domain.entity.Payment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 付款记录 Mapper
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
