package com.autorecon.service.impl;

import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.enums.BillStatusEnum;
import com.autorecon.mapper.ReconBillMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AutoConfirmScheduler {

    private final ReconBillMapper reconBillMapper;

    /**
     * Every hour, check for PENDING bills past their auto-confirm deadline
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional(rollbackFor = Exception.class)
    public void autoConfirmExpiredBills() {
        List<ReconBill> expiredBills = reconBillMapper.selectList(
                new LambdaQueryWrapper<ReconBill>()
                        .eq(ReconBill::getStatus, BillStatusEnum.PENDING.getCode())
                        .isNotNull(ReconBill::getAutoConfirmDeadline)
                        .le(ReconBill::getAutoConfirmDeadline, LocalDateTime.now())
                        .eq(ReconBill::getAutoConfirmed, 0)
                        .eq(ReconBill::getDeleted, 0)
        );

        for (ReconBill bill : expiredBills) {
            bill.setStatus(BillStatusEnum.TO_SIGN.getCode());
            bill.setAutoConfirmed(1);
            reconBillMapper.updateById(bill);
            log.info("Auto-confirmed bill: billId={}, billNo={}", bill.getId(), bill.getBillNo());
        }

        if (!expiredBills.isEmpty()) {
            log.info("Auto-confirmed {} expired bills", expiredBills.size());
        }
    }
}
