package com.autorecon.service;

import com.autorecon.domain.entity.SignRecord;

import java.util.List;

/**
 * 签章服务接口（Phase 2 占位）
 */
public interface SignService {

    Long initiateSignFlow(Long billId, Integer signOrderType);

    void executeSign(Long signRecordId, Long sealId, String verifyCode);

    SignRecord getSignStatus(Long billId);

    List<SignRecord> listPendingSignRecords(Long enterpriseId);

    void refuseSignFlow(Long signRecordId, String reason);

    void cancelSignFlow(Long signRecordId);
}
