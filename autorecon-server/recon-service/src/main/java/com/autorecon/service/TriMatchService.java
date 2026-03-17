package com.autorecon.service;

import com.autorecon.domain.vo.TriMatchVO;

/**
 * 三方匹配服务接口
 */
public interface TriMatchService {

    TriMatchVO getTriMatch(Long billId);
}
