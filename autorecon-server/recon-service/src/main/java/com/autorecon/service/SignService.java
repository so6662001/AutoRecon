package com.autorecon.service;

import com.autorecon.domain.entity.SignRecord;

/**
 * 签章服务接口（Phase 2 占位）
 */
public interface SignService {

    Long initiateSignFlow(Long billId, Integer signOrderType);

    void executeSign(Long signRecordId, Long sealId, String verifyCode);

    SignRecord getSignStatus(Long billId);
}
