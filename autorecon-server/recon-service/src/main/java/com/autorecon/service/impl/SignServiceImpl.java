package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.SignRecord;
import com.autorecon.domain.enums.SignStatusEnum;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.mapper.SignRecordMapper;
import com.autorecon.service.SignService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 签章服务实现（Phase 2 占位）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignServiceImpl implements SignService {

    private final SignRecordMapper signRecordMapper;
    private final ReconBillMapper reconBillMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long initiateSignFlow(Long billId, Integer signOrderType) {
        ReconBill bill = reconBillMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }

        String signFlowId = "SIGN_" + System.currentTimeMillis();
        SignRecord record = SignRecord.builder()
                .billId(billId)
                .signFlowId(signFlowId)
                .signOrderType(signOrderType != null ? signOrderType : 1)
                .sellerSignStatus(SignStatusEnum.PENDING.getValue())
                .buyerSignStatus(SignStatusEnum.PENDING.getValue())
                .overallStatus(SignStatusEnum.PENDING.getValue())
                .build();

        signRecordMapper.insert(record);
        log.info("Initiated sign flow: billId={}, signRecordId={}", billId, record.getId());
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeSign(Long signRecordId, Long sealId, String verifyCode) {
        SignRecord record = signRecordMapper.selectById(signRecordId);
        if (record == null) {
            throw new BizException(ErrorCode.SIGN_SEAL_NOT_FOUND.getCode(), "签章记录不存在");
        }

        // Placeholder: update sign status
        record.setSellerSignStatus(SignStatusEnum.SIGNED.getValue());
        record.setSellerSealId(sealId);
        record.setSellerSignAt(LocalDateTime.now());
        record.setOverallStatus(SignStatusEnum.SIGNED.getValue());
        record.setCompletedAt(LocalDateTime.now());
        signRecordMapper.updateById(record);

        ReconBill bill = reconBillMapper.selectById(record.getBillId());
        if (bill != null) {
            bill.setSellerSignStatus(SignStatusEnum.SIGNED.getValue());
            bill.setBuyerSignStatus(SignStatusEnum.SIGNED.getValue());
            reconBillMapper.updateById(bill);
        }

        log.info("Executed sign: signRecordId={}, sealId={}", signRecordId, sealId);
    }

    @Override
    public SignRecord getSignStatus(Long billId) {
        LambdaQueryWrapper<SignRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SignRecord::getBillId, billId).orderByDesc(SignRecord::getCreatedAt).last("LIMIT 1");
        return signRecordMapper.selectOne(wrapper);
    }

    @Override
    public List<SignRecord> listPendingSignRecords(Long enterpriseId) {
        LambdaQueryWrapper<ReconBill> billWrapper = new LambdaQueryWrapper<>();
        billWrapper.and(w -> w.eq(ReconBill::getSellerId, enterpriseId).or().eq(ReconBill::getBuyerId, enterpriseId));
        List<ReconBill> bills = reconBillMapper.selectList(billWrapper);
        if (bills == null || bills.isEmpty()) {
            return List.of();
        }
        List<Long> billIds = bills.stream().map(ReconBill::getId).toList();
        LambdaQueryWrapper<SignRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SignRecord::getBillId, billIds)
                .eq(SignRecord::getOverallStatus, SignStatusEnum.PENDING.getValue())
                .orderByDesc(SignRecord::getCreatedAt);
        return signRecordMapper.selectList(wrapper);
    }
}
