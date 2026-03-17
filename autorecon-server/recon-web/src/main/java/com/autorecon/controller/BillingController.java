package com.autorecon.controller;

import com.autorecon.common.result.PageResult;
import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.SubscribeDTO;
import com.autorecon.domain.entity.BillingRecord;
import com.autorecon.domain.entity.Subscription;
import com.autorecon.domain.vo.UsageVO;
import com.autorecon.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 计费管理 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "计费管理")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/current")
    @Operation(summary = "获取当前订阅")
    public R<Subscription> getCurrentSubscription() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        Subscription sub = billingService.getCurrentSubscription(enterpriseId);
        return R.ok(sub);
    }

    @PostMapping("/subscribe")
    @Operation(summary = "订阅")
    public R<Void> subscribe(@Valid @RequestBody SubscribeDTO dto) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        billingService.subscribe(enterpriseId, dto);
        return R.ok();
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消订阅")
    public R<Void> cancelSubscription() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        billingService.cancelSubscription(enterpriseId);
        return R.ok();
    }

    @GetMapping("/usage")
    @Operation(summary = "获取用量")
    public R<UsageVO> getUsage() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        UsageVO usage = billingService.getUsage(enterpriseId);
        return R.ok(usage);
    }

    @GetMapping("/bills")
    @Operation(summary = "账单列表")
    public R<PageResult<BillingRecord>> getBills(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        PageResult<BillingRecord> result = billingService.getBills(enterpriseId, pageNum, pageSize);
        return R.ok(result);
    }

    @PostMapping("/seal-package")
    @Operation(summary = "购买签章包")
    public R<Void> purchaseSealPackage(@RequestParam Integer packageType) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        billingService.purchaseSealPackage(enterpriseId, packageType);
        return R.ok();
    }

    @GetMapping("/seal-quota")
    @Operation(summary = "获取签章额度")
    public R<Integer> getSealQuota() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        Integer quota = billingService.getSealQuota(enterpriseId);
        return R.ok(quota);
    }
}
