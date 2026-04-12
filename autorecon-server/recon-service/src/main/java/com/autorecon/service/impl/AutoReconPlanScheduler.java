package com.autorecon.service.impl;

import com.autorecon.domain.entity.AutoReconPlan;
import com.autorecon.mapper.AutoReconPlanMapper;
import com.autorecon.service.AutoReconPlanService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AutoReconPlanScheduler {

    private final AutoReconPlanMapper autoReconPlanMapper;
    private final AutoReconPlanService autoReconPlanService;

    /**
     * Every hour, check for auto-recon plans that should be triggered
     */
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
            try {
                log.info("Auto-triggering recon plan: id={}, name={}", plan.getId(), plan.getPlanName());
                autoReconPlanService.triggerPlanInternal(plan.getId());

                AutoReconPlan updated = autoReconPlanMapper.selectById(plan.getId());
                if (updated == null) {
                    continue;
                }
                LocalDateTime nextExecution = calculateNextExecution(updated);
                updated.setNextExecuteAt(nextExecution);
                autoReconPlanMapper.updateById(updated);

                log.info("Plan {} triggered, next execution: {}", plan.getId(), nextExecution);
            } catch (Exception e) {
                log.error("Failed to trigger auto-recon plan {}: {}", plan.getId(), e.getMessage(), e);
            }
        }
    }

    private LocalDateTime calculateNextExecution(AutoReconPlan plan) {
        LocalDateTime now = LocalDateTime.now();
        Integer frequency = plan.getFrequency();
        if (frequency == null) {
            frequency = 1;
        }

        return switch (frequency) {
            case 1 -> now.plusMonths(1);
            case 2 -> now.plusDays(15);
            case 3 -> now.plusWeeks(1);
            case 4 -> now.plusMonths(3);
            default -> now.plusMonths(1);
        };
    }
}
