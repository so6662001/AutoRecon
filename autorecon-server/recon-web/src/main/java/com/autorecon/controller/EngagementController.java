package com.autorecon.controller;

import com.autorecon.common.result.PageResult;
import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.vo.BuyerEngagementVO;
import com.autorecon.domain.vo.EngagementFunnelVO;
import com.autorecon.service.EngagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 买方参与度 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/engagement")
@Tag(name = "买方参与度")
@RequiredArgsConstructor
public class EngagementController {

    private final EngagementService engagementService;

    @GetMapping("/buyers")
    @Operation(summary = "分页查询买方列表")
    public R<PageResult<BuyerEngagementVO>> listBuyers(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long sellerEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        PageResult<BuyerEngagementVO> result = engagementService.listBuyers(sellerEnterpriseId, pageNum, pageSize);
        return R.ok(result);
    }

    @GetMapping("/buyers/{id}")
    @Operation(summary = "获取买方详情")
    public R<BuyerEngagementVO> getBuyerDetail(@PathVariable Long id) {
        BuyerEngagementVO vo = engagementService.getBuyerDetail(id);
        return R.ok(vo);
    }

    @GetMapping("/funnel")
    @Operation(summary = "获取参与漏斗")
    public R<EngagementFunnelVO> getFunnel() {
        Long sellerEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        EngagementFunnelVO vo = engagementService.getFunnel(sellerEnterpriseId);
        return R.ok(vo);
    }

    @PostMapping("/invite")
    @Operation(summary = "发送邀请")
    public R<Void> sendInvite(@RequestParam Long engagementId) {
        engagementService.sendInvite(engagementId);
        return R.ok();
    }

    @PostMapping("/invite/batch")
    @Operation(summary = "批量发送邀请")
    public R<Void> batchInvite(@RequestBody List<Long> ids) {
        engagementService.batchInvite(ids);
        return R.ok();
    }
}
