package com.autorecon.service;

import com.autorecon.common.result.PageResult;
import com.autorecon.domain.entity.BuyerEngagement;
import com.autorecon.domain.vo.BuyerEngagementVO;
import com.autorecon.domain.vo.EngagementFunnelVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 买方参与度服务接口
 */
public interface EngagementService extends IService<BuyerEngagement> {

    PageResult<BuyerEngagementVO> listBuyers(Long sellerEnterpriseId, Integer pageNum, Integer pageSize);

    BuyerEngagementVO getBuyerDetail(Long id);

    EngagementFunnelVO getFunnel(Long sellerEnterpriseId);

    void sendInvite(Long engagementId);

    void batchInvite(List<Long> engagementIds);

    void updateEngagementLevel(Long buyerEnterpriseId, Long sellerEnterpriseId, Integer newLevel);

    void trackBillSent(Long buyerEnterpriseId, Long sellerEnterpriseId);

    void trackBillOpened(Long buyerEnterpriseId, Long sellerEnterpriseId);

    void trackBillConfirmed(Long buyerEnterpriseId, Long sellerEnterpriseId);
}
