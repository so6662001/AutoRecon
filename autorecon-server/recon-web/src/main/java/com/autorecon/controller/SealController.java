package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.SealCreateDTO;
import com.autorecon.domain.dto.SealOperatorCreateDTO;
import com.autorecon.domain.dto.SealOperatorUpdateDTO;
import com.autorecon.domain.entity.EnterpriseSeal;
import com.autorecon.domain.entity.SealOperator;
import com.autorecon.service.EnterpriseSealService;
import com.autorecon.service.SealOperatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 印章与经办人管理 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/sign")
@Tag(name = "印章与经办人管理")
@RequiredArgsConstructor
public class SealController {

    private final EnterpriseSealService enterpriseSealService;
    private final SealOperatorService sealOperatorService;

    // ========== 印章接口 ==========

    @PostMapping("/seals")
    @Operation(summary = "创建印章")
    public R<Long> createSeal(@Valid @RequestBody SealCreateDTO dto) {
        Long id = enterpriseSealService.createSeal(dto);
        return R.ok(id);
    }

    @GetMapping("/seals")
    @Operation(summary = "印章列表")
    public R<List<EnterpriseSeal>> listSeals(@RequestParam Long enterpriseId) {
        List<EnterpriseSeal> list = enterpriseSealService.listSeals(enterpriseId);
        return R.ok(list);
    }

    @PutMapping("/seals/{id}/disable")
    @Operation(summary = "停用印章")
    public R<Void> disableSeal(@PathVariable Long id) {
        enterpriseSealService.disableSeal(id);
        return R.ok();
    }

    @PutMapping("/seals/{id}/enable")
    @Operation(summary = "启用印章")
    public R<Void> enableSeal(@PathVariable Long id) {
        enterpriseSealService.enableSeal(id);
        return R.ok();
    }

    @DeleteMapping("/seals/{id}")
    @Operation(summary = "作废印章")
    public R<Void> revokeSeal(@PathVariable Long id) {
        enterpriseSealService.revokeSeal(id);
        return R.ok();
    }

    // ========== 经办人接口 ==========

    @PostMapping("/operators")
    @Operation(summary = "添加经办人")
    public R<Long> addOperator(@Valid @RequestBody SealOperatorCreateDTO dto) {
        Long id = sealOperatorService.addOperator(dto);
        return R.ok(id);
    }

    @GetMapping("/operators")
    @Operation(summary = "经办人列表")
    public R<List<SealOperator>> listOperators(@RequestParam Long enterpriseId) {
        List<SealOperator> list = sealOperatorService.listOperators(enterpriseId);
        return R.ok(list);
    }

    @PutMapping("/operators/{id}")
    @Operation(summary = "更新经办人")
    public R<Void> updateOperator(@PathVariable Long id, @Valid @RequestBody SealOperatorUpdateDTO dto) {
        sealOperatorService.updateOperator(id, dto);
        return R.ok();
    }

    @PutMapping("/operators/{id}/disable")
    @Operation(summary = "停用经办人")
    public R<Void> disableOperator(@PathVariable Long id) {
        sealOperatorService.disableOperator(id);
        return R.ok();
    }
}
