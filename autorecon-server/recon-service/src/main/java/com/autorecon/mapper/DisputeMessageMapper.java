package com.autorecon.mapper;

import com.autorecon.domain.entity.DisputeMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 异议沟通记录 Mapper
 */
@Mapper
public interface DisputeMessageMapper extends BaseMapper<DisputeMessage> {
}
