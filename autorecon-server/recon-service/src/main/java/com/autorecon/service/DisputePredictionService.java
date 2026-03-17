package com.autorecon.service;

import com.autorecon.domain.vo.DisputePredictionVO;

/**
 * 异议预测服务接口
 */
public interface DisputePredictionService {

    DisputePredictionVO predict(Long billId);
}
