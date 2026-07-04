package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.domain.entity.SignApproval;
import com.autorecon.mapper.SignApprovalMapper;
import com.autorecon.service.SignApprovalService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 签章审批服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignApprovalServiceImpl extends ServiceImpl<SignApprovalMapper, SignApproval> implements SignApprovalService {

    private final SignApprovalMapper signApprovalMapper;

    @Override
    public List<SignApproval> listPendingApprovals(Long approverId) {
        LambdaQueryWrapper<SignApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SignApproval::getApproverId, approverId)
                .eq(SignApproval::getApprovalStatus, 0)
                .orderByDesc(SignApproval::getAppliedAt);
        return signApprovalMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long approvalId, String comment) {
        SignApproval approval = signApprovalMapper.selectById(approvalId);
        if (approval == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "审批记录不存在");
        }
        approval.setApprovalStatus(1);
        approval.setApprovedAt(LocalDateTime.now());
        approval.setApprovalComment(comment);
        signApprovalMapper.updateById(approval);
        log.info("Approved sign: approvalId={}", approvalId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long approvalId, String comment) {
        SignApproval approval = signApprovalMapper.selectById(approvalId);
        if (approval == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "审批记录不存在");
        }
        approval.setApprovalStatus(2);
        approval.setApprovedAt(LocalDateTime.now());
        approval.setApprovalComment(comment);
        signApprovalMapper.updateById(approval);
        log.info("Rejected sign: approvalId={}", approvalId);
    }
}
