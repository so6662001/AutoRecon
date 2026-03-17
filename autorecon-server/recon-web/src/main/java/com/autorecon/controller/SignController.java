package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.domain.entity.SignRecord;
import com.autorecon.service.SignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
}
