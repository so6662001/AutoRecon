package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.AutoReconPlanCreateDTO;
import com.autorecon.domain.entity.AutoReconPlan;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.mapper.AutoReconPlanMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.AutoReconPlanService;
import com.autorecon.service.ReconBillService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 自动对账计划服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoReconPlanServiceImpl extends ServiceImpl<AutoReconPlanMapper, AutoReconPlan> implements AutoReconPlanService {

    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_DISABLED = 0;

    private final ReconBillService reconBillService;
    private final ReconBillMapper reconBillMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPlan(AutoReconPlanCreateDTO dto) {
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        if (sellerId == null) sellerId = 1L;

        String cronExpression = buildCronExpression(dto.getFrequency(), dto.getExecutionDay(), dto.getExecutionTime());

        AutoReconPlan plan = AutoReconPlan.builder()
                .sellerId(sellerId)
                .buyerId(dto.getBuyerId())
                .planName(dto.getPlanName())
                .frequency(dto.getFrequency())
                .executionDay(dto.getExecutionDay())
                .executionTime(dto.getExecutionTime())
                .templateId(dto.getTemplateId())
                .periodType(dto.getPeriodType())
                .autoSend(dto.getAutoSend())
                .includePayment(dto.getIncludePayment())
                .status(STATUS_ACTIVE)
                .cronExpression(cronExpression)
                .build();

        baseMapper.insert(plan);
        log.info("Created auto recon plan: id={}", plan.getId());
        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlan(Long id, AutoReconPlanCreateDTO dto) {
        AutoReconPlan plan = baseMapper.selectById(id);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "计划不存在");
        }

        plan.setPlanName(dto.getPlanName());
        plan.setBuyerId(dto.getBuyerId());
        plan.setFrequency(dto.getFrequency());
        plan.setExecutionDay(dto.getExecutionDay());
        plan.setExecutionTime(dto.getExecutionTime());
        plan.setTemplateId(dto.getTemplateId());
        plan.setPeriodType(dto.getPeriodType());
        plan.setAutoSend(dto.getAutoSend());
        plan.setIncludePayment(dto.getIncludePayment());
        plan.setCronExpression(buildCronExpression(dto.getFrequency(), dto.getExecutionDay(), dto.getExecutionTime()));
        baseMapper.updateById(plan);
        log.info("Updated auto recon plan: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void togglePlan(Long id) {
        AutoReconPlan plan = baseMapper.selectById(id);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "计划不存在");
        }
        int newStatus = STATUS_ACTIVE == (plan.getStatus() != null ? plan.getStatus() : 0) ? STATUS_DISABLED : STATUS_ACTIVE;
        plan.setStatus(newStatus);
        baseMapper.updateById(plan);
        log.info("Toggled auto recon plan: id={}, status={}", id, newStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerPlan(Long id) {
        AutoReconPlan plan = baseMapper.selectById(id);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "计划不存在");
        }
        if (plan.getTemplateId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "计划未配置模板");
        }

        LocalDate periodStart;
        LocalDate periodEnd;
        LocalDate now = LocalDate.now();
        int periodType = plan.getPeriodType() != null ? plan.getPeriodType() : 1;
        switch (periodType) {
            case 1 -> {
                periodStart = now.withDayOfMonth(1).minusMonths(1);
                periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
            }
            case 2 -> {
                periodStart = now.withDayOfMonth(1);
                periodEnd = now;
            }
            default -> {
                periodStart = now.withDayOfMonth(1).minusMonths(1);
                periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
            }
        }

        List<Long> buyerIds = new ArrayList<>();
        if (plan.getBuyerId() != null) {
            buyerIds.add(plan.getBuyerId());
        } else {
            LambdaQueryWrapper<ReconBill> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ReconBill::getSellerId, plan.getSellerId());
            List<ReconBill> bills = reconBillMapper.selectList(wrapper);
            buyerIds.addAll(bills.stream().map(ReconBill::getBuyerId).filter(b -> b != null).distinct().toList());
        }

        if (buyerIds.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "无可用买方");
        }

        reconBillService.batchCreateBills(buyerIds, periodStart, periodEnd, plan.getTemplateId());
        plan.setLastExecutedAt(LocalDateTime.now());
        baseMapper.updateById(plan);
        log.info("Triggered auto recon plan: id={}", id);
    }

    @Override
    public List<AutoReconPlan> listPlans(Long sellerId) {
        LambdaQueryWrapper<AutoReconPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoReconPlan::getSellerId, sellerId).orderByDesc(AutoReconPlan::getCreatedAt);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public AutoReconPlan getPlanDetail(Long id) {
        AutoReconPlan plan = baseMapper.selectById(id);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "计划不存在");
        }
        return plan;
    }

    private String buildCronExpression(Integer frequency, Integer executionDay, String executionTime) {
        int freq = frequency != null ? frequency : 1;
        int day = executionDay != null ? executionDay : 1;
        String time = executionTime != null && !executionTime.isEmpty() ? executionTime : "09:00";

        String[] parts = time.split(":");
        int hour = parts.length > 0 ? parseInt(parts[0], 9) : 9;
        int minute = parts.length > 1 ? parseInt(parts[1], 0) : 0;

        return switch (freq) {
            case 1 -> String.format("0 %d %d * * ?", minute, hour);
            case 2 -> String.format("0 %d %d ? * %d", minute, hour, Math.min(day, 7));
            case 3 -> String.format("0 %d %d %d * ?", minute, hour, Math.min(Math.max(day, 1), 28));
            default -> String.format("0 %d %d * * ?", minute, hour);
        };
    }

    private int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
