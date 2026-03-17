package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.entity.CreditScore;
import com.autorecon.domain.vo.CreditScoreDetailVO;
import com.autorecon.service.CreditScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 信用评分 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/credit")
@Tag(name = "信用评分")
@RequiredArgsConstructor
public class CreditScoreController {

    private final CreditScoreService creditScoreService;

    @GetMapping("/{buyerId}")
    @Operation(summary = "获取信用评分")
    public R<CreditScore> getScore(@PathVariable Long buyerId) {
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        CreditScore score = creditScoreService.getScore(buyerId, sellerId);
        return R.ok(score);
    }

    @GetMapping("/{buyerId}/detail")
    @Operation(summary = "获取信用评分详情")
    public R<CreditScoreDetailVO> getScoreDetail(@PathVariable Long buyerId) {
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        CreditScoreDetailVO vo = creditScoreService.getScoreDetail(buyerId, sellerId);
        return R.ok(vo);
    }

    @GetMapping("/{buyerId}/trend")
    @Operation(summary = "获取信用评分趋势")
    public R<List<CreditScore>> getTrend(@PathVariable Long buyerId) {
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        List<CreditScore> list = creditScoreService.getTrend(buyerId, sellerId);
        return R.ok(list);
    }

    @PutMapping("/{buyerId}/adjust")
    @Operation(summary = "调整信用评分")
    public R<Void> adjustScore(
            @PathVariable Long buyerId,
            @RequestParam BigDecimal newScore,
            @RequestParam(required = false) String reason) {
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        creditScoreService.adjustScore(buyerId, sellerId, newScore, reason);
        return R.ok();
    }

    @GetMapping("/ranking")
    @Operation(summary = "信用评分排名")
    public R<List<CreditScore>> getRanking(@RequestParam(defaultValue = "20") Integer limit) {
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        List<CreditScore> list = creditScoreService.getRanking(sellerId, limit);
        return R.ok(list);
    }

    @PostMapping("/{buyerId}/recalculate")
    @Operation(summary = "重新计算信用评分")
    public R<Void> recalculateScore(@PathVariable Long buyerId) {
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        creditScoreService.recalculateScore(buyerId, sellerId);
        return R.ok();
    }
}
