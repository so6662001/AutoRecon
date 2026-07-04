package com.autorecon.service.impl;

import com.autorecon.domain.entity.AutoReconPlan;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.mapper.AutoReconPlanMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.AuditLogService;
import com.autorecon.service.AutoReconPlanService;
import com.autorecon.service.ReconBillService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动对账计划调度器 (设计文档8.9)
 *
 * 每小时扫描到期的活跃计划 → 触发 → 失败重试(最多3次) → 自动发送(如配置) → 记录日志 → 计算下次执行
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AutoReconPlanScheduler {

    private static final int MAX_RETRY = 3;

    private final AutoReconPlanMapper autoReconPlanMapper;
    private final AutoReconPlanService autoReconPlanService;
    private final ReconBillService reconBillService;
    private final ReconBillMapper reconBillMapper;
    private final AuditLogService auditLogService;

    @Scheduled(fixedRate = 3600000)
    public void checkAndTriggerPlans() {
        List<AutoReconPlan> plans = autoReconPlanMapper.selectList(
                new LambdaQueryWrapper<AutoReconPlan>()
                        .eq(AutoReconPlan::getStatus, 1)
                        .isNotNull(AutoReconPlan::getNextExecuteAt)
                        .le(AutoReconPlan::getNextExecuteAt, LocalDateTime.now())
                        .eq(AutoReconPlan::getDeleted, 0)
        );

        for (AutoReconPlan plan : plans) {
            executePlanWithRetry(plan);
        }
    }

    /**
     * 执行计划(含失败重试)
     */
    private void executePlanWithRetry(AutoReconPlan plan) {
        String batchId = null;
        Exception lastError = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                log.info("Auto-triggering recon plan: id={}, name={}, attempt={}/{}",
                        plan.getId(), plan.getPlanName(), attempt, MAX_RETRY);

                autoReconPlanService.triggerPlanInternal(plan.getId());

                // 执行成功 — 查找本次生成的对账单
                AutoReconPlan updated = autoReconPlanMapper.selectById(plan.getId());
                if (updated == null) return;

                // 如果配置了自动发送，批量发送生成的对账单
                if (updated.getAutoSend() != null && updated.getAutoSend() == 1) {
                    autoSendBills(updated);
                }

                // 计算下次执行时间
                LocalDateTime nextExecution = calculateNextExecution(updated);
                updated.setNextExecuteAt(nextExecution);
                autoReconPlanMapper.updateById(updated);

                // 记录执行日志
                logExecution(plan, true, "执行成功", null);

                log.info("Plan {} triggered successfully, next execution: {}", plan.getId(), nextExecution);
                return; // 成功，退出重试

            } catch (Exception e) {
                lastError = e;
                log.warn("Auto-recon plan {} attempt {}/{} failed: {}",
                        plan.getId(), attempt, MAX_RETRY, e.getMessage());

                if (attempt < MAX_RETRY) {
                    try { Thread.sleep(5000L * attempt); } catch (InterruptedException ignored) {} // 递增等待
                }
            }
        }

        // 全部重试失败
        log.error("Auto-recon plan {} failed after {} retries", plan.getId(), MAX_RETRY, lastError);
        logExecution(plan, false, "执行失败(重试" + MAX_RETRY + "次): " + (lastError != null ? lastError.getMessage() : ""), null);

        // 仍然计算下次执行(避免反复重试同一个失败计划)
        AutoReconPlan updated = autoReconPlanMapper.selectById(plan.getId());
        if (updated != null) {
            updated.setNextExecuteAt(calculateNextExecution(updated));
            autoReconPlanMapper.updateById(updated);
        }
    }

    /**
     * 自动发送: 查找计划关联的最近生成的对账单并发送
     */
    private void autoSendBills(AutoReconPlan plan) {
        try {
            // 查找最近1小时内该卖方创建的CREATED状态对账单
            List<ReconBill> recentBills = reconBillMapper.selectList(
                    new LambdaQueryWrapper<ReconBill>()
                            .eq(ReconBill::getSellerId, plan.getSellerId())
                            .eq(ReconBill::getStatus, "CREATED")
                            .ge(ReconBill::getCreatedAt, LocalDateTime.now().minusHours(1))
                            .eq(ReconBill::getDeleted, 0));

            int sent = 0;
            for (ReconBill bill : recentBills) {
                try {
                    reconBillService.sendBill(bill.getId());
                    sent++;
                } catch (Exception e) {
                    log.warn("Auto-send failed for bill {}: {}", bill.getId(), e.getMessage());
                }
            }
            log.info("Auto-sent {} bills for plan {}", sent, plan.getId());
        } catch (Exception e) {
            log.warn("Auto-send failed for plan {}: {}", plan.getId(), e.getMessage());
        }
    }

    /**
     * 记录执行日志到审计表
     */
    private void logExecution(AutoReconPlan plan, boolean success, String detail, String extra) {
        try {
            auditLogService.log("AUTO_RECON_PLAN",
                    success ? "execute_success" : "execute_fail",
                    "AutoReconPlan", plan.getId(),
                    "计划[" + plan.getPlanName() + "] " + detail);
        } catch (Exception e) {
            log.warn("Failed to write plan execution log: {}", e.getMessage());
        }
    }

    private LocalDateTime calculateNextExecution(AutoReconPlan plan) {
        LocalDateTime now = LocalDateTime.now();
        Integer frequency = plan.getFrequency();
        if (frequency == null) frequency = 1;

        return switch (frequency) {
            case 1 -> now.plusMonths(1);   // 每月
            case 2 -> now.plusDays(15);    // 每半月
            case 3 -> now.plusWeeks(1);    // 每周
            case 4 -> now.plusMonths(3);   // 每季度
            default -> now.plusMonths(1);
        };
    }
}
