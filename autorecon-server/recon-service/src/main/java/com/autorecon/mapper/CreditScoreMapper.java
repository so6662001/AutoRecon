package com.autorecon.mapper;

import com.autorecon.domain.entity.CreditScore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 催收信用评分 Mapper
 */
@Mapper
public interface CreditScoreMapper extends BaseMapper<CreditScore> {
}
