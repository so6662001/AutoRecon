package com.autorecon.service;

import com.autorecon.domain.entity.SignApproval;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 签章审批服务接口
 */
public interface SignApprovalService extends IService<SignApproval> {

    List<SignApproval> listPendingApprovals(Long approverId);

    void approve(Long approvalId, String comment);

    void reject(Long approvalId, String comment);
}
