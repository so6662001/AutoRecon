package com.autorecon.controller;

import com.autorecon.common.result.PageResult;
import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.FinanceApplyDTO;
import com.autorecon.domain.entity.FinanceApply;
import com.autorecon.service.FinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 融资 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/finance")
@Tag(name = "融资")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @PostMapping("/apply")
    @Operation(summary = "提交融资申请")
    public R<Long> apply(@Valid @RequestBody FinanceApplyDTO dto) {
        Long applyId = financeService.apply(dto);
        return R.ok(applyId);
    }

    @GetMapping("/{applyId}")
    @Operation(summary = "获取融资申请详情")
    public R<FinanceApply> getApplyDetail(@PathVariable Long applyId) {
        FinanceApply apply = financeService.getApplyDetail(applyId);
        return R.ok(apply);
    }

    @GetMapping("/")
    @Operation(summary = "分页查询融资申请列表")
    public R<PageResult<FinanceApply>> listApplies(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        PageResult<FinanceApply> result = financeService.listApplies(sellerId, status, pageNum, pageSize);
        return R.ok(result);
    }

    @GetMapping("/eligible-bills")
    @Operation(summary = "获取可融资对账单ID列表")
    public R<List<Long>> getEligibleBillIds() {
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        List<Long> billIds = financeService.getEligibleBillIds(sellerId);
        return R.ok(billIds);
    }

    @PutMapping("/{applyId}/status")
    @Operation(summary = "更新融资申请状态")
    public R<Void> updateApplyStatus(
            @PathVariable Long applyId,
            @RequestParam Integer status,
            @RequestParam(required = false) BigDecimal approvedAmount) {
        financeService.updateApplyStatus(applyId, status, approvedAmount);
        return R.ok();
    }
}
