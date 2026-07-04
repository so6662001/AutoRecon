package com.autorecon.service.impl;

import com.autorecon.domain.entity.AuditLog;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.enums.BillStatusEnum;
import com.autorecon.mapper.AuditLogMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.AuditLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 超时自动确认调度器 (设计文档8.7)
 *
 * 每小时执行:
 * 1. 到期前提醒(距截止24h内且未提醒的)
 * 2. 到期后自动确认(排除已提异议的)
 * 3. 记录审计日志
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AutoConfirmScheduler {

    private final ReconBillMapper reconBillMapper;
    private final AuditLogService auditLogService;

    /**
     * 每小时: 发送到期前提醒 + 执行超时自动确认
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional(rollbackFor = Exception.class)
    public void executeAutoConfirmCycle() {
        sendExpiryReminders();
        autoConfirmExpiredBills();
    }

    /**
     * 到期前提醒: 距截止时间≤24小时且仍为PENDING的对账单
     */
    private void sendExpiryReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindow = now.plusHours(24);

        List<ReconBill> soonExpiring = reconBillMapper.selectList(
                new LambdaQueryWrapper<ReconBill>()
                        .eq(ReconBill::getStatus, BillStatusEnum.PENDING.getCode())
                        .isNotNull(ReconBill::getAutoConfirmDeadline)
                        .gt(ReconBill::getAutoConfirmDeadline, now) // 还没到期
                        .le(ReconBill::getAutoConfirmDeadline, reminderWindow) // 但24h内到期
                        .eq(ReconBill::getAutoConfirmed, 0)
                        .eq(ReconBill::getDeleted, 0)
        );

        for (ReconBill bill : soonExpiring) {
            long hoursLeft = ChronoUnit.HOURS.between(now, bill.getAutoConfirmDeadline());
            log.info("Auto-confirm reminder: billId={}, billNo={}, hoursLeft={}",
                    bill.getId(), bill.getBillNo(), hoursLeft);
            // TODO: 发送提醒通知(短信+站内信)
            // notificationService.sendAutoConfirmReminder(bill, hoursLeft);
        }

        if (!soonExpiring.isEmpty()) {
            log.info("Sent {} auto-confirm reminders (bills expiring within 24h)", soonExpiring.size());
        }
    }

    /**
     * 超时自动确认: 排除已提异议(DISPUTED)的对账单
     */
    private void autoConfirmExpiredBills() {
        List<ReconBill> expiredBills = reconBillMapper.selectList(
                new LambdaQueryWrapper<ReconBill>()
                        .eq(ReconBill::getStatus, BillStatusEnum.PENDING.getCode()) // 仅PENDING，排除DISPUTED
                        .isNotNull(ReconBill::getAutoConfirmDeadline)
                        .le(ReconBill::getAutoConfirmDeadline, LocalDateTime.now())
                        .eq(ReconBill::getAutoConfirmed, 0)
                        .eq(ReconBill::getDeleted, 0)
        );

        for (ReconBill bill : expiredBills) {
            bill.setStatus(BillStatusEnum.TO_SIGN.getCode());
            bill.setAutoConfirmed(1);
            reconBillMapper.updateById(bill);

            // 记录审计日志
            try {
                auditLogService.log("AUTO_CONFIRM", "auto_confirm",
                        "ReconBill", bill.getId(),
                        "对账单" + bill.getBillNo() + "超时自动确认(截止时间:" + bill.getAutoConfirmDeadline() + ")");
            } catch (Exception e) {
                log.warn("Failed to write audit log for auto-confirm: {}", e.getMessage());
            }

            log.info("Auto-confirmed bill: billId={}, billNo={}, deadline={}",
                    bill.getId(), bill.getBillNo(), bill.getAutoConfirmDeadline());

            // TODO: 通知买方"已自动确认"
            // notificationService.sendAutoConfirmedNotification(bill);
        }

        if (!expiredBills.isEmpty()) {
            log.info("Auto-confirmed {} expired bills", expiredBills.size());
        }
    }
}
