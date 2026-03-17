package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.vo.MatchResultVO;
import com.autorecon.service.MatchEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 匹配引擎 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/match")
@Tag(name = "匹配引擎")
@RequiredArgsConstructor
public class MatchController {

    private final MatchEngineService matchEngineService;

    @PostMapping("/{billId}/execute")
    @Operation(summary = "执行匹配")
    public R<MatchResultVO> executeMatch(@PathVariable Long billId) {
        MatchResultVO result = matchEngineService.executeMatch(billId);
        return R.ok(result);
    }

    @GetMapping("/{billId}/result")
    @Operation(summary = "获取匹配结果")
    public R<MatchResultVO> getMatchResult(@PathVariable Long billId) {
        MatchResultVO result = matchEngineService.getMatchResult(billId);
        return R.ok(result);
    }

    @PostMapping("/{billId}/re-match")
    @Operation(summary = "重新匹配")
    public R<Void> rematch(@PathVariable Long billId) {
        matchEngineService.rematch(billId);
        return R.ok();
    }

    @GetMapping("/{billId}/diff")
    @Operation(summary = "获取差异明细")
    public R<List<ReconBillItem>> getDiffItems(@PathVariable Long billId) {
        List<ReconBillItem> items = matchEngineService.getDiffItems(billId);
        return R.ok(items);
    }
}
