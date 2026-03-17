package com.autorecon.mapper;

import com.autorecon.domain.entity.BillingRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账单记录 Mapper
 */
@Mapper
public interface BillingRecordMapper extends BaseMapper<BillingRecord> {
}
