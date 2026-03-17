package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.common.util.TenantUtil;
import com.autorecon.domain.dto.DisputeCreateDTO;
import com.autorecon.domain.dto.DisputeMessageDTO;
import com.autorecon.domain.entity.Dispute;
import com.autorecon.domain.entity.DisputeMessage;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.enums.BillStatusEnum;
import com.autorecon.mapper.DisputeMapper;
import com.autorecon.mapper.DisputeMessageMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.DisputeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异议服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeServiceImpl extends ServiceImpl<DisputeMapper, Dispute> implements DisputeService {

    private static final int DISPUTE_STATUS_OPEN = 0;
    private static final int DISPUTE_STATUS_RESOLVED = 1;

    private final DisputeMapper disputeMapper;
    private final DisputeMessageMapper disputeMessageMapper;
    private final ReconBillMapper reconBillMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDispute(DisputeCreateDTO dto) {
        ReconBill bill = reconBillMapper.selectById(dto.getBillId());
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());

        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) userId = 1L;

        Dispute dispute = Dispute.builder()
                .billId(dto.getBillId())
                .billItemId(dto.getBillItemId())
                .disputeType(dto.getDisputeType())
                .description(dto.getDescription())
                .raisedBy(userId)
                .raisedBySide(1)
                .status(DISPUTE_STATUS_OPEN)
                .build();

        disputeMapper.insert(dispute);

        bill.setStatus(BillStatusEnum.DISPUTED.getCode());
        reconBillMapper.updateById(bill);

        log.info("Created dispute: id={}, billId={}", dispute.getId(), dto.getBillId());
        return dispute.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMessage(DisputeMessageDTO dto) {
        Dispute dispute = disputeMapper.selectById(dto.getDisputeId());
        if (dispute == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "异议记录不存在");
        }
        ReconBill bill = reconBillMapper.selectById(dispute.getBillId());
        if (bill != null) {
            TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());
        }

        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) userId = 1L;

        DisputeMessage message = DisputeMessage.builder()
                .disputeId(dto.getDisputeId())
                .senderId(userId)
                .senderSide(1)
                .messageType(dto.getMessageType())
                .content(dto.getContent())
                .attachmentUrl(dto.getAttachmentUrl())
                .readStatus(0)
                .build();

        disputeMessageMapper.insert(message);
        log.info("Sent dispute message: disputeId={}", dto.getDisputeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveDispute(Long disputeId, String resolution) {
        Dispute dispute = disputeMapper.selectById(disputeId);
        if (dispute == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "异议记录不存在");
        }
        ReconBill bill = reconBillMapper.selectById(dispute.getBillId());
        if (bill != null) {
            TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());
        }

        dispute.setStatus(DISPUTE_STATUS_RESOLVED);
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setResolution(resolution);
        disputeMapper.updateById(dispute);

        LambdaQueryWrapper<Dispute> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dispute::getBillId, dispute.getBillId()).eq(Dispute::getStatus, DISPUTE_STATUS_OPEN);
        Long openCount = disputeMapper.selectCount(wrapper);

        if (openCount == 0) {
            bill = reconBillMapper.selectById(dispute.getBillId());
            if (bill != null) {
                bill.setStatus(BillStatusEnum.TO_SIGN.getCode());
                reconBillMapper.updateById(bill);
                log.info("All disputes resolved, bill status updated to TO_SIGN: billId={}", bill.getId());
            }
        }

        log.info("Resolved dispute: disputeId={}", disputeId);
    }

    @Override
    public List<Dispute> listDisputes(Long billId) {
        LambdaQueryWrapper<Dispute> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dispute::getBillId, billId).orderByDesc(Dispute::getCreatedAt);
        return disputeMapper.selectList(wrapper);
    }

    @Override
    public List<DisputeMessage> getMessages(Long disputeId) {
        LambdaQueryWrapper<DisputeMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DisputeMessage::getDisputeId, disputeId).orderByAsc(DisputeMessage::getCreatedAt);
        return disputeMessageMapper.selectList(wrapper);
    }
}
