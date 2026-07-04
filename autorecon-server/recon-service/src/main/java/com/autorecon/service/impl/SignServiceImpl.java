package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.common.util.TenantUtil;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.SignRecord;
import com.autorecon.domain.enums.BillStatusEnum;
import com.autorecon.domain.enums.SignStatusEnum;
import com.autorecon.domain.entity.EnterpriseSeal;
import com.autorecon.mapper.EnterpriseSealMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.mapper.SignRecordMapper;
import com.autorecon.service.CollectionService;
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
    private final EnterpriseSealMapper enterpriseSealMapper;
    private final CollectionService collectionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long initiateSignFlow(Long billId, Integer signOrderType) {
        ReconBill bill = reconBillMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());
        if (!BillStatusEnum.TO_SIGN.getCode().equals(bill.getStatus())) {
            throw new BizException(ErrorCode.BILL_STATUS_ERROR);
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
        if (verifyCode == null || verifyCode.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "验证码不能为空");
        }
        ReconBill bill = reconBillMapper.selectById(record.getBillId());
        if (bill != null) {
            TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());
        }
        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId != null && sealId != null) {
            EnterpriseSeal seal = enterpriseSealMapper.selectById(sealId);
            if (seal != null && !currentEnterpriseId.equals(seal.getEnterpriseId())) {
                throw new BizException(ErrorCode.SIGN_SEAL_NOT_FOUND.getCode(), "印章不属于当前企业");
            }
        }

        if (bill == null || currentEnterpriseId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!BillStatusEnum.TO_SIGN.getCode().equals(bill.getStatus())) {
            throw new BizException(ErrorCode.BILL_STATUS_ERROR);
        }

        boolean isSeller = currentEnterpriseId.equals(bill.getSellerId());
        boolean isBuyer = currentEnterpriseId.equals(bill.getBuyerId());
        if (!isSeller && !isBuyer) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        LocalDateTime now = LocalDateTime.now();

        if (isSeller) {
            record.setSellerSignStatus(SignStatusEnum.SIGNED.getValue());
            record.setSellerSealId(sealId);
            record.setSellerSignAt(now);
            bill.setSellerSignStatus(SignStatusEnum.SIGNED.getValue());
        } else {
            record.setBuyerSignStatus(SignStatusEnum.SIGNED.getValue());
            record.setBuyerSealId(sealId);
            record.setBuyerSignAt(now);
            bill.setBuyerSignStatus(SignStatusEnum.SIGNED.getValue());
        }

        int sellerStatus = record.getSellerSignStatus() != null ? record.getSellerSignStatus() : SignStatusEnum.PENDING.getValue();
        int buyerStatus = record.getBuyerSignStatus() != null ? record.getBuyerSignStatus() : SignStatusEnum.PENDING.getValue();
        boolean bothSigned = sellerStatus == SignStatusEnum.SIGNED.getValue() && buyerStatus == SignStatusEnum.SIGNED.getValue();

        if (bothSigned) {
            record.setOverallStatus(SignStatusEnum.SIGNED.getValue());
            record.setCompletedAt(now);
            bill.setStatus(BillStatusEnum.SIGNED.getCode());
            try {
                collectionService.autoCreatePlanForBill(bill);
                bill.setStatus(BillStatusEnum.COLLECTING.getCode());
            } catch (Exception e) {
                log.warn("Failed to auto-create collection plan for bill {}", bill.getId(), e);
            }
        }
        signRecordMapper.updateById(record);
        reconBillMapper.updateById(bill);

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refuseSignFlow(Long signRecordId, String reason) {
        SignRecord record = signRecordMapper.selectById(signRecordId);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "签署记录不存在");
        }
        ReconBill bill = reconBillMapper.selectById(record.getBillId());
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());
        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        boolean isSeller = currentEnterpriseId.equals(bill.getSellerId());
        boolean isBuyer = currentEnterpriseId.equals(bill.getBuyerId());
        if (!isSeller && !isBuyer) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        int refused = SignStatusEnum.REFUSED.getValue();
        if (isSeller) {
            record.setSellerSignStatus(refused);
        } else {
            record.setBuyerSignStatus(refused);
        }
        record.setOverallStatus(refused);
        signRecordMapper.updateById(record);

        bill.setStatus(BillStatusEnum.DISPUTED.getCode());
        reconBillMapper.updateById(bill);
        log.info("Refused sign flow: signRecordId={}, reason={}", signRecordId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSignFlow(Long signRecordId) {
        SignRecord record = signRecordMapper.selectById(signRecordId);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "签署记录不存在");
        }
        ReconBill bill = reconBillMapper.selectById(record.getBillId());
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());
        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!currentEnterpriseId.equals(bill.getSellerId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "仅发起方可撤销签署流程");
        }
        record.setOverallStatus(SignStatusEnum.CANCELLED.getValue());
        signRecordMapper.updateById(record);
        log.info("Cancelled sign flow: signRecordId={}", signRecordId);
    }
}
