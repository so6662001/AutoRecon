package com.pickupexpress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickupexpress.domain.entity.LiftRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发货吊装记录 Mapper
 */
@Mapper
public interface LiftRecordMapper extends BaseMapper<LiftRecord> {
}
