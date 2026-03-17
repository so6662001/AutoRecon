package com.pickupexpress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickupexpress.domain.entity.SupplementRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 事后补录记录 Mapper
 */
@Mapper
public interface SupplementRecordMapper extends BaseMapper<SupplementRecord> {
}
