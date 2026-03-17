package com.autorecon.service;

import com.autorecon.common.result.PageResult;
import com.autorecon.domain.dto.CollectionPlanCreateDTO;
import com.autorecon.domain.entity.CollectionLog;
import com.autorecon.domain.entity.CollectionPlan;
import com.autorecon.domain.vo.CollectionPlanVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 催收计划服务接口
 */
public interface CollectionService extends IService<CollectionPlan> {

    Long createPlan(CollectionPlanCreateDTO dto);

    PageResult<CollectionPlanVO> listPlans(Long sellerId, Integer status, Integer pageNum, Integer pageSize);

    void executePlan(Long planId, String actionType, String content);

    void registerPayment(Long planId, BigDecimal amount, String remark);

    void pausePlan(Long planId);

    void resumePlan(Long planId);

    CollectionPlan getPlanByBillId(Long billId);

    List<CollectionLog> listLogs(Long planId);
}
