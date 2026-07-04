package com.autorecon.mapper;

import com.autorecon.domain.entity.GuestAccessToken;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 访客访问令牌 Mapper
 */
@Mapper
public interface GuestAccessTokenMapper extends BaseMapper<GuestAccessToken> {
}
