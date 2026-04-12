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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @Operation(summary = "获取引导话术模板")
    @GetMapping("/scripts")
    public R<List<Map<String, String>>> getScripts() {
        List<Map<String, String>> scripts = new ArrayList<>();
        scripts.add(Map.of("scenario", "首次发送对账单", "script", "张总您好，我们公司上线了电子对账系统..."));
        scripts.add(Map.of("scenario", "引导注册", "script", "注册后可以查看所有历史对账记录..."));
        scripts.add(Map.of("scenario", "解释签章", "script", "电子签章是合法的，e签宝是全国最大的签章平台..."));
        scripts.add(Map.of("scenario", "买方担心安全", "script", "数据加密存储，只有双方能看到..."));
        return R.ok(scripts);
    }

    @Operation(summary = "获取线下辅助物料")
    @GetMapping("/materials")
    public R<List<Map<String, String>>> getMaterials() {
        List<Map<String, String>> materials = new ArrayList<>();
        materials.add(Map.of("name", "平台使用一页纸指南", "type", "pdf", "url", "/materials/quick-guide.pdf"));
        materials.add(Map.of("name", "电子签章法律效力说明", "type", "pdf", "url", "/materials/legal-statement.pdf"));
        materials.add(Map.of("name", "数据安全承诺函", "type", "pdf", "url", "/materials/security-commitment.pdf"));
        materials.add(Map.of("name", "3分钟上手视频", "type", "video", "url", "/materials/tutorial-video.mp4"));
        return R.ok(materials);
    }
}
