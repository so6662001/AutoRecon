package com.autorecon.service;

import com.autorecon.common.result.PageResult;
import com.autorecon.domain.dto.FinanceApplyDTO;
import com.autorecon.domain.entity.FinanceApply;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 融资服务接口
 */
public interface FinanceService extends IService<FinanceApply> {

    Long apply(FinanceApplyDTO dto);

    FinanceApply getApplyDetail(Long applyId);

    PageResult<FinanceApply> listApplies(Long sellerId, Integer status, Integer pageNum, Integer pageSize);

    List<Long> getEligibleBillIds(Long sellerId);

    void updateApplyStatus(Long applyId, Integer status, BigDecimal approvedAmount);
}
