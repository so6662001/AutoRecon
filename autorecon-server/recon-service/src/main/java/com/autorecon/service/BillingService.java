package com.autorecon.service;

import com.autorecon.common.result.PageResult;
import com.autorecon.domain.dto.SubscribeDTO;
import com.autorecon.domain.entity.BillingRecord;
import com.autorecon.domain.entity.Subscription;
import com.autorecon.domain.vo.UsageVO;

/**
 * 计费服务接口
 */
public interface BillingService {

    Subscription getCurrentSubscription(Long enterpriseId);

    void subscribe(Long enterpriseId, SubscribeDTO dto);

    void cancelSubscription(Long enterpriseId);

    UsageVO getUsage(Long enterpriseId);

    PageResult<BillingRecord> getBills(Long enterpriseId, Integer pageNum, Integer pageSize);

    void purchaseSealPackage(Long enterpriseId, Integer packageType);

    Integer getSealQuota(Long enterpriseId);
}
