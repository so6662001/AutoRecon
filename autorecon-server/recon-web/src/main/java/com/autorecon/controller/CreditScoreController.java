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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            @RequestParam(value = "newScore", required = false) BigDecimal newScore,
            @RequestParam(value = "score", required = false) BigDecimal score,
            @RequestParam(required = false) String reason) {
        BigDecimal actualScore = newScore != null ? newScore : score;
        if (actualScore == null) return R.fail("评分不能为空");
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        creditScoreService.adjustScore(buyerId, sellerId, actualScore, reason);
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

    @Operation(summary = "评分因子明细")
    @GetMapping("/{buyerId}/factors")
    public R<Map<String, Object>> getFactors(@PathVariable Long buyerId) {
        CreditScore score = creditScoreService.getScore(buyerId, SecurityUtil.getCurrentEnterpriseId());
        if (score == null) {
            return R.fail("未找到信用评分");
        }
        Map<String, Object> factors = new HashMap<>();
        if (score.getScoreFactors() != null) {
            factors.put("rawFactors", score.getScoreFactors());
        }
        factors.put("score", score.getCreditScore());
        factors.put("level", score.getScoreLevel());
        factors.put("avgPaymentDays", score.getAvgPaymentDays());
        factors.put("overdueRate", score.getOverdueRate());
        factors.put("disputeRate", score.getDisputeRate());
        return R.ok(factors);
    }
}
