package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.NotificationTemplateDTO;
import com.autorecon.domain.dto.ReconRulesDTO;
import com.autorecon.domain.dto.TimeoutConfigDTO;
import com.autorecon.service.EnterpriseSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Enterprise-level system settings (timeout, notifications, recon rules).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "系统设置扩展")
@RequiredArgsConstructor
public class SystemSettingsController {

    private final EnterpriseSettingsService enterpriseSettingsService;

    @GetMapping("/timeout-config")
    @Operation(summary = "获取超时确认规则")
    public R<TimeoutConfigDTO> getTimeoutConfig() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        return R.ok(enterpriseSettingsService.getTimeoutConfig(enterpriseId));
    }

    @PutMapping("/timeout-config")
    @Operation(summary = "保存超时确认规则")
    public R<Void> saveTimeoutConfig(@Valid @RequestBody TimeoutConfigDTO dto) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        enterpriseSettingsService.saveTimeoutConfig(enterpriseId, dto);
        return R.ok();
    }

    @GetMapping("/notification-templates")
    @Operation(summary = "获取通知模板")
    public R<NotificationTemplateDTO> getNotificationTemplates() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        return R.ok(enterpriseSettingsService.getNotificationTemplates(enterpriseId));
    }

    @PutMapping("/notification-templates")
    @Operation(summary = "保存通知模板")
    public R<Void> saveNotificationTemplates(@Valid @RequestBody NotificationTemplateDTO dto) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        enterpriseSettingsService.saveNotificationTemplates(enterpriseId, dto);
        return R.ok();
    }

    @GetMapping("/recon-rules")
    @Operation(summary = "获取对账规则")
    public R<ReconRulesDTO> getReconRules() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        return R.ok(enterpriseSettingsService.getReconRules(enterpriseId));
    }

    @PutMapping("/recon-rules")
    @Operation(summary = "保存对账规则")
    public R<Void> saveReconRules(@Valid @RequestBody ReconRulesDTO dto) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        enterpriseSettingsService.saveReconRules(enterpriseId, dto);
        return R.ok();
    }
}
