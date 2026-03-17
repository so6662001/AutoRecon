package com.autorecon.controller;

import com.autorecon.common.result.PageResult;
import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.entity.AuditLog;
import com.autorecon.domain.vo.SecurityStatusVO;
import com.autorecon.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 数据安全与主权 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/security")
@Tag(name = "数据安全与主权")
@RequiredArgsConstructor
public class SecurityController {

    private final AuditLogService auditLogService;

    @GetMapping("/status")
    @Operation(summary = "获取安全状态")
    public R<SecurityStatusVO> getStatus() {
        SecurityStatusVO vo = new SecurityStatusVO();
        vo.setDataIsolation(true);
        vo.setEncryptionLevel("AES-256");
        vo.setTransferEncryption("TLS 1.3");
        vo.setLastBackupAt(LocalDateTime.now().minusHours(6));
        vo.setBackupFrequency("每日");
        vo.setTotalAccessCount(0);
        return R.ok(vo);
    }

    @GetMapping("/access-log")
    @Operation(summary = "访问日志")
    public R<PageResult<AuditLog>> getAccessLog(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        PageResult<AuditLog> result = auditLogService.queryLogs(enterpriseId, module, pageNum, pageSize);
        return R.ok(result);
    }
}
