package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.CalendarEventDTO;
import com.autorecon.domain.entity.AutoReconPlan;
import com.autorecon.domain.entity.CollectionPlan;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.ReconCalendar;
import com.autorecon.mapper.AutoReconPlanMapper;
import com.autorecon.mapper.CollectionPlanMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.mapper.ReconCalendarMapper;
import com.autorecon.service.ReconCalendarService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 对账日历服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ReconCalendarServiceImpl extends ServiceImpl<ReconCalendarMapper, ReconCalendar> implements ReconCalendarService {

    private final ReconCalendarMapper reconCalendarMapper;
    private final ReconBillMapper reconBillMapper;
    private final AutoReconPlanMapper autoReconPlanMapper;
    private final CollectionPlanMapper collectionPlanMapper;

    @Override
    public List<ReconCalendar> getEvents(Long enterpriseId, Integer year, Integer month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        LambdaQueryWrapper<ReconCalendar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconCalendar::getEnterpriseId, enterpriseId)
                .ge(ReconCalendar::getEventDate, monthStart)
                .le(ReconCalendar::getEventDate, monthEnd)
                .orderByAsc(ReconCalendar::getEventDate);
        return reconCalendarMapper.selectList(wrapper);
    }

    @Override
    public List<ReconCalendar> getUpcoming(Long enterpriseId, Integer days) {
        LocalDate now = LocalDate.now();
        LocalDate endDate = now.plusDays(days);

        LambdaQueryWrapper<ReconCalendar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconCalendar::getEnterpriseId, enterpriseId)
                .ge(ReconCalendar::getEventDate, now)
                .le(ReconCalendar::getEventDate, endDate)
                .eq(ReconCalendar::getStatus, 0)
                .orderByAsc(ReconCalendar::getEventDate);
        return reconCalendarMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEvent(CalendarEventDTO dto) {
        if (dto == null || dto.getEventDate() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        ReconCalendar event = ReconCalendar.builder()
                .enterpriseId(enterpriseId)
                .eventType(dto.getEventType())
                .eventDate(dto.getEventDate())
                .eventTitle(dto.getEventTitle())
                .relatedBillId(dto.getRelatedBillId())
                .relatedBuyerId(dto.getRelatedBuyerId())
                .status(0)
                .build();
        reconCalendarMapper.insert(event);
        log.info("Created calendar event: id={}, enterpriseId={}", event.getId(), enterpriseId);
        return event.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEventStatus(Long eventId, Integer status) {
        ReconCalendar event = reconCalendarMapper.selectById(eventId);
        if (event == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "日历事件不存在");
        }
        event.setStatus(status);
        reconCalendarMapper.updateById(event);
        log.info("Updated calendar event status: eventId={}, status={}", eventId, status);
    }

    // ========== 自动生成日历事件 (设计文档8.10) ==========

    /**
     * 每天凌晨1点: 为所有活跃企业自动生成未来30天的日历事件
     * 事件来源: 自动对账计划 / 签章到期 / 付款到期 / 催收节点
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void autoGenerateEvents() {
        log.info("Auto-generating calendar events...");

        // 获取所有活跃卖方
        List<ReconBill> recentBills = reconBillMapper.selectList(
                new LambdaQueryWrapper<ReconBill>()
                        .ge(ReconBill::getCreatedAt, LocalDateTime.now().minusMonths(3))
                        .eq(ReconBill::getDeleted, 0)
                        .select(ReconBill::getSellerId)
                        .groupBy(ReconBill::getSellerId));

        int totalEvents = 0;
        for (ReconBill b : recentBills) {
            if (b.getSellerId() == null) continue;
            try {
                totalEvents += generateEventsForEnterprise(b.getSellerId());
            } catch (Exception e) {
                log.warn("Failed to generate events for enterprise {}: {}", b.getSellerId(), e.getMessage());
            }
        }
        log.info("Auto-generated {} calendar events", totalEvents);
    }

    /**
     * 手动触发: 为指定企业生成日历事件
     */
    public int generateEventsForEnterprise(Long enterpriseId) {
        List<ReconCalendar> newEvents = new ArrayList<>();
        LocalDate now = LocalDate.now();
        LocalDate futureLimit = now.plusDays(30);

        // 1. 📋 计划对账事件 — 从AutoReconPlan的nextExecuteAt提取
        List<AutoReconPlan> activePlans = autoReconPlanMapper.selectList(
                new LambdaQueryWrapper<AutoReconPlan>()
                        .eq(AutoReconPlan::getSellerId, enterpriseId)
                        .eq(AutoReconPlan::getStatus, 1)
                        .isNotNull(AutoReconPlan::getNextExecuteAt)
                        .eq(AutoReconPlan::getDeleted, 0));
        for (AutoReconPlan plan : activePlans) {
            LocalDate planDate = plan.getNextExecuteAt().toLocalDate();
            if (!planDate.isBefore(now) && !planDate.isAfter(futureLimit)) {
                if (!eventExists(enterpriseId, 1, planDate, plan.getId())) {
                    newEvents.add(ReconCalendar.builder()
                            .enterpriseId(enterpriseId)
                            .eventType(1) // 计划对账
                            .eventDate(planDate)
                            .eventTitle("📋 " + plan.getPlanName())
                            .relatedBillId(plan.getId())
                            .status(0)
                            .build());
                }
            }
        }

        // 2. ⏰ 签章/确认到期事件 — 从ReconBill的autoConfirmDeadline提取
        List<ReconBill> pendingBills = reconBillMapper.selectList(
                new LambdaQueryWrapper<ReconBill>()
                        .eq(ReconBill::getSellerId, enterpriseId)
                        .eq(ReconBill::getStatus, "PENDING")
                        .isNotNull(ReconBill::getAutoConfirmDeadline)
                        .eq(ReconBill::getAutoConfirmed, 0)
                        .eq(ReconBill::getDeleted, 0));
        for (ReconBill bill : pendingBills) {
            LocalDate deadlineDate = bill.getAutoConfirmDeadline().toLocalDate();
            if (!deadlineDate.isBefore(now) && !deadlineDate.isAfter(futureLimit)) {
                if (!eventExists(enterpriseId, 2, deadlineDate, bill.getId())) {
                    newEvents.add(ReconCalendar.builder()
                            .enterpriseId(enterpriseId)
                            .eventType(2) // 签章/确认到期
                            .eventDate(deadlineDate)
                            .eventTitle("⏰ " + bill.getBillNo() + " 确认到期")
                            .relatedBillId(bill.getId())
                            .relatedBuyerId(bill.getBuyerId())
                            .status(0)
                            .build());
                }
            }
        }

        // 3. 💰 付款到期事件 — 从CollectionPlan的dueDate提取
        List<CollectionPlan> activeCollections = collectionPlanMapper.selectList(
                new LambdaQueryWrapper<CollectionPlan>()
                        .eq(CollectionPlan::getSellerId, enterpriseId)
                        .eq(CollectionPlan::getStatus, 1) // 活跃
                        .isNotNull(CollectionPlan::getDueDate)
                        .eq(CollectionPlan::getDeleted, 0));
        for (CollectionPlan cp : activeCollections) {
            if (!cp.getDueDate().isBefore(now) && !cp.getDueDate().isAfter(futureLimit)) {
                if (!eventExists(enterpriseId, 3, cp.getDueDate(), cp.getBillId())) {
                    newEvents.add(ReconCalendar.builder()
                            .enterpriseId(enterpriseId)
                            .eventType(3) // 付款到期
                            .eventDate(cp.getDueDate())
                            .eventTitle("💰 付款到期(应收¥" + (cp.getRemainingAmount() != null ? cp.getRemainingAmount().toPlainString() : "0") + ")")
                            .relatedBillId(cp.getBillId())
                            .relatedBuyerId(cp.getBuyerId())
                            .status(0)
                            .build());
                }
            }

            // 4. ⚠ 催收节点 — nextActionDate
            if (cp.getNextActionDate() != null && !cp.getNextActionDate().isBefore(now) && !cp.getNextActionDate().isAfter(futureLimit)) {
                if (!eventExists(enterpriseId, 4, cp.getNextActionDate(), cp.getBillId())) {
                    newEvents.add(ReconCalendar.builder()
                            .enterpriseId(enterpriseId)
                            .eventType(4) // 催收节点
                            .eventDate(cp.getNextActionDate())
                            .eventTitle("⚠ 催收提醒(第" + (cp.getCurrentStage() != null ? cp.getCurrentStage() : 1) + "阶段)")
                            .relatedBillId(cp.getBillId())
                            .relatedBuyerId(cp.getBuyerId())
                            .status(0)
                            .build());
                }
            }
        }

        // 批量插入
        for (ReconCalendar event : newEvents) {
            reconCalendarMapper.insert(event);
        }

        if (!newEvents.isEmpty()) {
            log.info("Generated {} events for enterprise {}", newEvents.size(), enterpriseId);
        }
        return newEvents.size();
    }

    /**
     * 检查事件是否已存在(避免重复生成)
     */
    private boolean eventExists(Long enterpriseId, Integer eventType, LocalDate eventDate, Long relatedId) {
        Long count = reconCalendarMapper.selectCount(
                new LambdaQueryWrapper<ReconCalendar>()
                        .eq(ReconCalendar::getEnterpriseId, enterpriseId)
                        .eq(ReconCalendar::getEventType, eventType)
                        .eq(ReconCalendar::getEventDate, eventDate)
                        .eq(ReconCalendar::getRelatedBillId, relatedId)
                        .eq(ReconCalendar::getDeleted, 0));
        return count != null && count > 0;
    }
}
