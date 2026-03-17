package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.entity.SignApproval;
import com.autorecon.domain.entity.SignRecord;
import com.autorecon.service.SignApprovalService;
import com.autorecon.service.SignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 签章管理 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/sign")
@Tag(name = "签章管理")
@RequiredArgsConstructor
public class SignController {

    private final SignService signService;
    private final SignApprovalService signApprovalService;

    @PostMapping("/flows")
    @Operation(summary = "发起签章流程")
    public R<Long> initiateSignFlow(
            @RequestParam Long billId,
            @RequestParam(defaultValue = "1") Integer signOrderType) {
        Long flowId = signService.initiateSignFlow(billId, signOrderType);
        return R.ok(flowId);
    }

    @PostMapping("/flows/{id}/sign")
    @Operation(summary = "执行签章")
    public R<Void> executeSign(
            @PathVariable Long id,
            @RequestParam Long sealId,
            @RequestParam String verifyCode) {
        signService.executeSign(id, sealId, verifyCode);
        return R.ok();
    }

    @GetMapping("/flows/{billId}/status")
    @Operation(summary = "获取签章状态")
    public R<SignRecord> getSignStatus(@PathVariable Long billId) {
        SignRecord record = signService.getSignStatus(billId);
        return R.ok(record);
    }

    @GetMapping("/pending")
    @Operation(summary = "待签章列表")
    public R<List<SignRecord>> listPendingSignRecords() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        List<SignRecord> list = signService.listPendingSignRecords(enterpriseId);
        return R.ok(list);
    }

    @GetMapping("/approvals/pending")
    @Operation(summary = "待审批列表")
    public R<List<SignApproval>> listPendingApprovals() {
        Long approverId = SecurityUtil.getCurrentUserId();
        List<SignApproval> list = signApprovalService.listPendingApprovals(approverId);
        return R.ok(list);
    }

    @PutMapping("/approvals/{id}/approve")
    @Operation(summary = "审批通过")
    public R<Void> approve(@PathVariable Long id, @RequestParam(required = false) String comment) {
        signApprovalService.approve(id, comment);
        return R.ok();
    }

    @PutMapping("/approvals/{id}/reject")
    @Operation(summary = "审批拒绝")
    public R<Void> reject(@PathVariable Long id, @RequestParam(required = false) String comment) {
        signApprovalService.reject(id, comment);
        return R.ok();
    }
}
