package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.AutoReconPlanCreateDTO;
import com.autorecon.domain.entity.AutoReconPlan;
import com.autorecon.service.AutoReconPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自动对账计划 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/auto-plans")
@Tag(name = "自动对账计划")
@RequiredArgsConstructor
public class AutoReconPlanController {

    private final AutoReconPlanService autoReconPlanService;

    @PostMapping("/")
    @Operation(summary = "创建计划")
    public R<Long> createPlan(@Valid @RequestBody AutoReconPlanCreateDTO dto) {
        Long id = autoReconPlanService.createPlan(dto);
        return R.ok(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新计划")
    public R<Void> updatePlan(@PathVariable Long id, @Valid @RequestBody AutoReconPlanCreateDTO dto) {
        autoReconPlanService.updatePlan(id, dto);
        return R.ok();
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "切换计划启用状态")
    public R<Void> togglePlan(@PathVariable Long id) {
        autoReconPlanService.togglePlan(id);
        return R.ok();
    }

    @PostMapping("/{id}/trigger")
    @Operation(summary = "手动触发计划")
    public R<Void> triggerPlan(@PathVariable Long id) {
        autoReconPlanService.triggerPlan(id);
        return R.ok();
    }

    @GetMapping("/")
    @Operation(summary = "列出计划")
    public R<List<AutoReconPlan>> listPlans(@RequestParam(required = false) Long sellerId) {
        Long sid = sellerId != null ? sellerId : SecurityUtil.getCurrentEnterpriseId();
        if (sid == null) sid = 1L;
        List<AutoReconPlan> list = autoReconPlanService.listPlans(sid);
        return R.ok(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取计划详情")
    public R<AutoReconPlan> getPlanDetail(@PathVariable Long id) {
        AutoReconPlan plan = autoReconPlanService.getPlanDetail(id);
        return R.ok(plan);
    }
}
