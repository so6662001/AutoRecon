package com.pickupexpress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickupexpress.domain.entity.ProgressEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 进度事件 Mapper
 */
@Mapper
public interface ProgressEventMapper extends BaseMapper<ProgressEvent> {
}
