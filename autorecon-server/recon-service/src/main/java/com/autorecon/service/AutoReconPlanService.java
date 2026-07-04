package com.autorecon.service;

import com.autorecon.domain.dto.AutoReconPlanCreateDTO;
import com.autorecon.domain.entity.AutoReconPlan;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 自动对账计划服务接口
 */
public interface AutoReconPlanService extends IService<AutoReconPlan> {

    /**
     * 创建计划
     */
    Long createPlan(AutoReconPlanCreateDTO dto);

    /**
     * 更新计划
     */
    void updatePlan(Long id, AutoReconPlanCreateDTO dto);

    /**
     * 切换计划启用状态
     */
    void togglePlan(Long id);

    /**
     * 手动触发计划
     */
    void triggerPlan(Long id);

    /**
     * 系统触发（如定时任务），跳过租户归属校验。
     */
    void triggerPlanInternal(Long id);

    /**
     * 列出计划
     */
    List<AutoReconPlan> listPlans(Long sellerId);

    /**
     * 获取计划详情
     */
    AutoReconPlan getPlanDetail(Long id);
}
