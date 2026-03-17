package com.autorecon.controller;

import com.autorecon.common.result.PageResult;
import com.autorecon.common.result.R;
import com.autorecon.domain.dto.CollectionPlanCreateDTO;
import com.autorecon.domain.entity.CollectionLog;
import com.autorecon.domain.entity.CollectionPlan;
import com.autorecon.domain.vo.CollectionPlanVO;
import com.autorecon.service.CollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 催收管理 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/collection")
@Tag(name = "催收管理")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @PostMapping("/plans")
    @Operation(summary = "创建催收计划")
    public R<Long> createPlan(@Valid @RequestBody CollectionPlanCreateDTO dto) {
        Long id = collectionService.createPlan(dto);
        return R.ok(id);
    }

    @GetMapping("/plans")
    @Operation(summary = "催收计划列表")
    public R<PageResult<CollectionPlanVO>> listPlans(
            @RequestParam Long sellerId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<CollectionPlanVO> result = collectionService.listPlans(sellerId, status, pageNum, pageSize);
        return R.ok(result);
    }

    @PostMapping("/plans/{id}/execute")
    @Operation(summary = "执行催收动作")
    public R<Void> executePlan(
            @PathVariable Long id,
            @RequestParam String actionType,
            @RequestParam(required = false) String content) {
        collectionService.executePlan(id, actionType, content != null ? content : "");
        return R.ok();
    }

    @PostMapping("/plans/{id}/register-payment")
    @Operation(summary = "登记回款")
    public R<Void> registerPayment(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String remark) {
        collectionService.registerPayment(id, amount, remark);
        return R.ok();
    }

    @PutMapping("/plans/{id}/pause")
    @Operation(summary = "暂停催收计划")
    public R<Void> pausePlan(@PathVariable Long id) {
        collectionService.pausePlan(id);
        return R.ok();
    }

    @PutMapping("/plans/{id}/resume")
    @Operation(summary = "恢复催收计划")
    public R<Void> resumePlan(@PathVariable Long id) {
        collectionService.resumePlan(id);
        return R.ok();
    }

    @GetMapping("/plans/bill/{billId}")
    @Operation(summary = "根据对账单获取催收计划")
    public R<CollectionPlan> getPlanByBillId(@PathVariable Long billId) {
        CollectionPlan plan = collectionService.getPlanByBillId(billId);
        return R.ok(plan);
    }

    @GetMapping("/plans/{id}/logs")
    @Operation(summary = "催收执行记录")
    public R<List<CollectionLog>> listLogs(@PathVariable Long id) {
        List<CollectionLog> list = collectionService.listLogs(id);
        return R.ok(list);
    }
}
