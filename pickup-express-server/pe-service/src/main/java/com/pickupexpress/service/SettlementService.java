package com.pickupexpress.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pickupexpress.common.result.PageResult;
import com.pickupexpress.domain.entity.SettlementOrder;
import com.pickupexpress.domain.vo.SettlementVO;

import java.util.List;

/**
 * 结算服务接口
 */
public interface SettlementService extends IService<SettlementOrder> {

    Long generateSettlement(Long pickupOrderId);

    SettlementVO getSettlement(Long settlementId);

    void markCustomerViewed(Long settlementId);

    List<SettlementOrder> listByContract(Long contractId);

    PageResult<SettlementVO> listSettlements(String settlementNo, Long contractId, Integer status, Integer pageNum, Integer pageSize);
}
