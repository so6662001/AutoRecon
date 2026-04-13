package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.entity.ToleranceLearn;
import com.autorecon.service.ToleranceLearnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 容差学习 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/tolerance")
@Tag(name = "容差学习")
@RequiredArgsConstructor
public class ToleranceLearnController {

    private final ToleranceLearnService toleranceLearnService;

    @GetMapping
    @Operation(summary = "获取待处理容差建议（GET /v1/recon/tolerance）")
    public R<List<ToleranceLearn>> listPendingSuggestions() {
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        List<ToleranceLearn> list = toleranceLearnService.listAll(sellerId);
        list = list.stream()
                .filter(l -> l.getAdopted() == null || l.getAdopted() == 0)
                .toList();
        return R.ok(list);
    }

    @GetMapping("/all")
    @Operation(summary = "获取全部容差学习记录")
    public R<List<ToleranceLearn>> listAll() {
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        List<ToleranceLearn> list = toleranceLearnService.listAll(sellerId);
        return R.ok(list);
    }

    @GetMapping("/suggestions")
    @Operation(summary = "获取容差建议")
    public R<List<ToleranceLearn>> getSuggestions(@RequestParam Long buyerId) {
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        List<ToleranceLearn> list = toleranceLearnService.getSuggestions(sellerId, buyerId);
        return R.ok(list);
    }

    @PutMapping("/{id}/adopt")
    @Operation(summary = "采纳建议")
    public R<Void> adoptSuggestion(@PathVariable Long id) {
        toleranceLearnService.adoptSuggestion(id);
        return R.ok();
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "拒绝建议")
    public R<Void> rejectSuggestion(@PathVariable Long id) {
        toleranceLearnService.rejectSuggestion(id);
        return R.ok();
    }

    @PostMapping("/analyze")
    @Operation(summary = "手动触发容差分析(管理员)")
    public R<String> triggerAnalysis() {
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        ((com.autorecon.service.impl.ToleranceLearnServiceImpl) toleranceLearnService).manualAnalyze(sellerId);
        return R.ok("容差分析已完成，请查看建议列表");
    }
}
