package com.autorecon.mapper;

import com.autorecon.domain.entity.Dispute;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 异议记录 Mapper
 */
@Mapper
public interface DisputeMapper extends BaseMapper<Dispute> {
}
