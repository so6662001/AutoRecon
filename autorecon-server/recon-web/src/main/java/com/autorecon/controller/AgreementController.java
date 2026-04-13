package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.AgreementConfirmDTO;
import com.autorecon.domain.dto.AgreementPublishDTO;
import com.autorecon.domain.entity.AgreementConfirmation;
import com.autorecon.domain.entity.AgreementVersion;
import com.autorecon.domain.vo.AgreementStatusVO;
import com.autorecon.service.AgreementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agreements")
@Tag(name = "协议管理")
@RequiredArgsConstructor
@Slf4j
public class AgreementController {

    private final AgreementService agreementService;

    @Operation(summary = "发布新版协议(管理员)")
    @PostMapping("/publish")
    public R<Long> publish(@Valid @RequestBody AgreementPublishDTO dto) {
        return R.ok(agreementService.publishAgreement(dto));
    }

    @Operation(summary = "查询协议版本列表")
    @GetMapping("/versions")
    public R<List<AgreementVersion>> listVersions(@RequestParam(required = false) Integer agreementType) {
        return R.ok(agreementService.listVersions(agreementType));
    }

    @Operation(summary = "查询协议版本详情")
    @GetMapping("/versions/{id}")
    public R<AgreementVersion> getVersion(@PathVariable Long id) {
        return R.ok(agreementService.getVersion(id));
    }

    @Operation(summary = "废弃协议版本(管理员)")
    @PutMapping("/versions/{id}/deprecate")
    public R<Void> deprecateVersion(@PathVariable Long id) {
        agreementService.deprecateVersion(id);
        return R.ok();
    }

    @Operation(summary = "获取当前生效版本")
    @GetMapping("/current")
    public R<AgreementVersion> getCurrentVersion(@RequestParam Integer agreementType) {
        return R.ok(agreementService.getCurrentVersion(agreementType));
    }

    @Operation(summary = "检查用户是否需要确认协议")
    @GetMapping("/check")
    public R<AgreementStatusVO> checkStatus() {
        Long userId = SecurityUtil.getCurrentUserId();
        return R.ok(agreementService.checkAgreementStatus(userId));
    }

    @Operation(summary = "用户确认协议")
    @PostMapping("/confirm")
    public R<Void> confirm(@Valid @RequestBody AgreementConfirmDTO dto, HttpServletRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        agreementService.confirmAgreement(userId, dto, request.getRemoteAddr(), request.getHeader("User-Agent"));
        return R.ok();
    }

    @Operation(summary = "查询用户确认历史")
    @GetMapping("/confirmations")
    public R<List<AgreementConfirmation>> getConfirmations() {
        Long userId = SecurityUtil.getCurrentUserId();
        return R.ok(agreementService.getUserConfirmations(userId));
    }
}
