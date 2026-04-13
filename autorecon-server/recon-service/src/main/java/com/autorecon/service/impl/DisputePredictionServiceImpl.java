package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.TenantUtil;
import com.autorecon.domain.entity.Dispute;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.vo.DisputePredictionVO;
import com.autorecon.mapper.DisputeMapper;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.CreditScoreService;
import com.autorecon.service.DisputePredictionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 异议预测服务 — 基于多维特征的规则引擎
 *
 * 特征维度(对应设计文档8.2):
 * ├── 交易特征: 金额大小、单价波动、重量差异历史、退货关联
 * ├── 客户特征: 历史异议率、常见异议类型、信用评分
 * ├── 时间特征: 月末/月初、距上次对账间隔
 * └── 匹配特征: 过磅vs理论偏差、已有差异标记
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisputePredictionServiceImpl implements DisputePredictionService {

    private final ReconBillMapper reconBillMapper;
    private final ReconBillItemMapper reconBillItemMapper;
    private final DisputeMapper disputeMapper;
    private final CreditScoreService creditScoreService;

    @Override
    public DisputePredictionVO predict(Long billId) {
        ReconBill bill = reconBillMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());

        List<ReconBillItem> items = reconBillItemMapper.selectList(
                new LambdaQueryWrapper<ReconBillItem>()
                        .eq(ReconBillItem::getBillId, billId)
                        .orderByAsc(ReconBillItem::getLineNo));

        // Pre-compute buyer context (shared across all items)
        BuyerContext ctx = buildBuyerContext(bill);

        List<DisputePredictionVO.PredictionItem> highRiskItems = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        int count = 0;

        for (ReconBillItem item : items) {
            List<String> reasons = new ArrayList<>();
            BigDecimal riskScore = calculateRiskScore(item, ctx, reasons);
            totalScore = totalScore.add(riskScore);
            count++;

            if (riskScore.compareTo(BigDecimal.valueOf(40)) >= 0) {
                DisputePredictionVO.PredictionItem pi = new DisputePredictionVO.PredictionItem();
                pi.setItemId(item.getId());
                pi.setLineNo(item.getLineNo());
                pi.setProductName(item.getProductName());
                pi.setSpec(item.getSpec());
                pi.setRiskScore(riskScore);
                pi.setRiskReason(String.join("；", reasons));
                highRiskItems.add(pi);
            }
        }

        BigDecimal overallScore = count > 0
                ? totalScore.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String riskLevel = overallScore.compareTo(BigDecimal.valueOf(70)) > 0 ? "高"
                : overallScore.compareTo(BigDecimal.valueOf(40)) >= 0 ? "中" : "低";

        DisputePredictionVO vo = new DisputePredictionVO();
        vo.setBillId(billId);
        vo.setOverallScore(overallScore);
        vo.setRiskLevel(riskLevel);
        vo.setHighRiskItems(highRiskItems);
        return vo;
    }

    // ========== Buyer-level context (computed once per bill) ==========

    private static class BuyerContext {
        BigDecimal buyerDisputeRate;       // 该客户历史异议率 (0~100)
        Map<String, BigDecimal> specDisputeRates; // 按品规的异议率
        BigDecimal creditScore;            // 信用评分
        boolean isMonthEnd;                // 月末(25号后)
        long daysSinceLastRecon;           // 距上次对账天数
        Map<String, BigDecimal> recentAvgPrices;  // 近期均价(按品规)
    }

    private BuyerContext buildBuyerContext(ReconBill bill) {
        BuyerContext ctx = new BuyerContext();

        // 1. Customer dispute rate
        long totalBills = reconBillMapper.selectCount(
                new LambdaQueryWrapper<ReconBill>()
                        .eq(ReconBill::getBuyerId, bill.getBuyerId())
                        .eq(ReconBill::getSellerId, bill.getSellerId())
                        .eq(ReconBill::getDeleted, 0));
        long disputedBills = reconBillMapper.selectCount(
                new LambdaQueryWrapper<ReconBill>()
                        .eq(ReconBill::getBuyerId, bill.getBuyerId())
                        .eq(ReconBill::getSellerId, bill.getSellerId())
                        .eq(ReconBill::getStatus, "DISPUTED")
                        .eq(ReconBill::getDeleted, 0));
        ctx.buyerDisputeRate = totalBills > 0
                ? BigDecimal.valueOf(disputedBills * 100.0 / totalBills).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 2. Per-spec dispute rates from historical disputes
        ctx.specDisputeRates = new HashMap<>();
        List<Dispute> historicalDisputes = disputeMapper.selectList(
                new LambdaQueryWrapper<Dispute>()
                        .eq(Dispute::getDeleted, 0)
                        .isNotNull(Dispute::getBillItemId));
        // Count disputes per productName+spec by joining with bill items
        Map<String, Integer> specDisputeCounts = new HashMap<>();
        for (Dispute d : historicalDisputes) {
            ReconBillItem dItem = reconBillItemMapper.selectById(d.getBillItemId());
            if (dItem != null) {
                ReconBill dBill = reconBillMapper.selectById(dItem.getBillId());
                if (dBill != null && dBill.getBuyerId().equals(bill.getBuyerId())) {
                    String key = (dItem.getProductName() != null ? dItem.getProductName() : "") + "|"
                            + (dItem.getSpec() != null ? dItem.getSpec() : "");
                    specDisputeCounts.merge(key, 1, Integer::sum);
                }
            }
        }
        // Count total items per spec for this buyer
        List<ReconBillItem> allBuyerItems = new ArrayList<>();
        List<ReconBill> buyerBills = reconBillMapper.selectList(
                new LambdaQueryWrapper<ReconBill>()
                        .eq(ReconBill::getBuyerId, bill.getBuyerId())
                        .eq(ReconBill::getSellerId, bill.getSellerId())
                        .eq(ReconBill::getDeleted, 0));
        for (ReconBill b : buyerBills) {
            allBuyerItems.addAll(reconBillItemMapper.selectList(
                    new LambdaQueryWrapper<ReconBillItem>().eq(ReconBillItem::getBillId, b.getId())));
        }
        Map<String, Long> specTotalCounts = allBuyerItems.stream()
                .collect(Collectors.groupingBy(
                        i -> (i.getProductName() != null ? i.getProductName() : "") + "|"
                                + (i.getSpec() != null ? i.getSpec() : ""),
                        Collectors.counting()));
        for (Map.Entry<String, Integer> e : specDisputeCounts.entrySet()) {
            long total = specTotalCounts.getOrDefault(e.getKey(), 1L);
            ctx.specDisputeRates.put(e.getKey(),
                    BigDecimal.valueOf(e.getValue() * 100.0 / total).setScale(1, RoundingMode.HALF_UP));
        }

        // 3. Recent average prices per spec (last 3 months)
        ctx.recentAvgPrices = new HashMap<>();
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        List<ReconBillItem> recentItems = allBuyerItems.stream()
                .filter(i -> i.getDeliveryDate() != null && i.getDeliveryDate().isAfter(threeMonthsAgo))
                .collect(Collectors.toList());
        Map<String, List<BigDecimal>> specPrices = new HashMap<>();
        for (ReconBillItem i : recentItems) {
            if (i.getUnitPrice() != null) {
                String key = (i.getProductName() != null ? i.getProductName() : "") + "|"
                        + (i.getSpec() != null ? i.getSpec() : "");
                specPrices.computeIfAbsent(key, k -> new ArrayList<>()).add(i.getUnitPrice());
            }
        }
        for (Map.Entry<String, List<BigDecimal>> e : specPrices.entrySet()) {
            BigDecimal avg = e.getValue().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(e.getValue().size()), 2, RoundingMode.HALF_UP);
            ctx.recentAvgPrices.put(e.getKey(), avg);
        }

        // 4. Credit score
        try {
            var cs = creditScoreService.getScore(bill.getBuyerId(), bill.getSellerId());
            ctx.creditScore = cs != null ? cs.getCreditScore() : BigDecimal.valueOf(70);
        } catch (Exception e) {
            ctx.creditScore = BigDecimal.valueOf(70);
        }

        // 5. Time features
        ctx.isMonthEnd = LocalDate.now().getDayOfMonth() >= 25;
        ReconBill lastBill = reconBillMapper.selectOne(
                new LambdaQueryWrapper<ReconBill>()
                        .eq(ReconBill::getBuyerId, bill.getBuyerId())
                        .eq(ReconBill::getSellerId, bill.getSellerId())
                        .ne(ReconBill::getId, bill.getId())
                        .eq(ReconBill::getDeleted, 0)
                        .orderByDesc(ReconBill::getPeriodEnd)
                        .last("LIMIT 1"));
        ctx.daysSinceLastRecon = lastBill != null && lastBill.getPeriodEnd() != null
                ? ChronoUnit.DAYS.between(lastBill.getPeriodEnd(), LocalDate.now()) : 30;

        return ctx;
    }

    // ========== Per-item risk score calculation ==========

    private BigDecimal calculateRiskScore(ReconBillItem item, BuyerContext ctx, List<String> reasons) {
        BigDecimal score = BigDecimal.ZERO;

        // ── 交易特征 ──

        // F1: 交易金额大小 (大额交易风险更高)
        if (item.getAmount() != null && item.getAmount().compareTo(BigDecimal.valueOf(100000)) > 0) {
            score = score.add(BigDecimal.valueOf(10));
            if (item.getAmount().compareTo(BigDecimal.valueOf(500000)) > 0) {
                score = score.add(BigDecimal.valueOf(5));
                reasons.add("大额交易(¥" + item.getAmount().setScale(0, RoundingMode.HALF_UP) + ")");
            }
        }

        // F2: 单价波动 (与近3月均价偏差)
        if (item.getUnitPrice() != null) {
            String specKey = (item.getProductName() != null ? item.getProductName() : "") + "|"
                    + (item.getSpec() != null ? item.getSpec() : "");
            BigDecimal avgPrice = ctx.recentAvgPrices.get(specKey);
            if (avgPrice != null && avgPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal priceDiff = item.getUnitPrice().subtract(avgPrice).abs();
                BigDecimal diffRate = priceDiff.divide(avgPrice, 4, RoundingMode.HALF_UP);
                if (diffRate.compareTo(BigDecimal.valueOf(0.05)) > 0) {
                    score = score.add(BigDecimal.valueOf(20));
                    reasons.add("单价偏差" + priceDiff.setScale(0, RoundingMode.HALF_UP) + "元/吨(偏离近3月均价"
                            + diffRate.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + "%)");
                } else if (diffRate.compareTo(BigDecimal.valueOf(0.02)) > 0) {
                    score = score.add(BigDecimal.valueOf(10));
                }
            }
        }

        // F3: 已有重量差异
        if (item.getDiffWeight() != null && item.getDiffWeight().abs().compareTo(BigDecimal.ZERO) > 0) {
            score = score.add(BigDecimal.valueOf(15));
            reasons.add("重量差异" + item.getDiffWeight().abs().setScale(2, RoundingMode.HALF_UP) + "吨");
        }

        // F4: 已有金额差异
        if (item.getDiffAmount() != null && item.getDiffAmount().abs().compareTo(BigDecimal.ZERO) > 0) {
            score = score.add(BigDecimal.valueOf(15));
        }

        // ── 客户特征 ──

        // F5: 该客户整体历史异议率
        if (ctx.buyerDisputeRate.compareTo(BigDecimal.valueOf(20)) > 0) {
            score = score.add(BigDecimal.valueOf(15));
        } else if (ctx.buyerDisputeRate.compareTo(BigDecimal.valueOf(10)) > 0) {
            score = score.add(BigDecimal.valueOf(8));
        }

        // F6: 该客户对此品规的历史异议率 (最关键的特征)
        String specKey = (item.getProductName() != null ? item.getProductName() : "") + "|"
                + (item.getSpec() != null ? item.getSpec() : "");
        BigDecimal specRate = ctx.specDisputeRates.get(specKey);
        if (specRate != null && specRate.compareTo(BigDecimal.valueOf(15)) > 0) {
            score = score.add(BigDecimal.valueOf(25));
            reasons.add("该客户历史上对" + item.getSpec() + " " + item.getProductName()
                    + "的异议率达" + specRate + "%");
        } else if (specRate != null && specRate.compareTo(BigDecimal.valueOf(5)) > 0) {
            score = score.add(BigDecimal.valueOf(10));
            reasons.add("该品规历史异议率" + specRate + "%");
        }

        // F7: 信用评分低的客户风险高
        if (ctx.creditScore.compareTo(BigDecimal.valueOf(60)) < 0) {
            score = score.add(BigDecimal.valueOf(10));
        }

        // ── 时间特征 ──

        // F8: 月末/月初 (财务结算期，审核更严格)
        if (ctx.isMonthEnd) {
            score = score.add(BigDecimal.valueOf(5));
        }

        // F9: 距上次对账间隔长 (积累的差异更多)
        if (ctx.daysSinceLastRecon > 45) {
            score = score.add(BigDecimal.valueOf(8));
            reasons.add("距上次对账" + ctx.daysSinceLastRecon + "天");
        }

        // ── 匹配特征 ──

        // F10: 已有归因标记
        if (item.getWeightDiffCause() != null && !item.getWeightDiffCause().isEmpty()) {
            if (item.getWeightDiffCause().contains("异常")) {
                score = score.add(BigDecimal.valueOf(20));
                reasons.add(item.getWeightDiffCause());
            } else if (item.getWeightDiffCause().contains("疑似")) {
                score = score.add(BigDecimal.valueOf(10));
            }
        }

        // F11: 已有人工标记的风险
        if (item.getDisputeRiskLevel() != null && item.getDisputeRiskLevel() > 0) {
            score = score.add(BigDecimal.valueOf(item.getDisputeRiskLevel() * 10));
        }

        // Ensure reasons are not empty
        if (reasons.isEmpty()) {
            if (score.compareTo(BigDecimal.valueOf(40)) >= 0) {
                reasons.add("综合风险因素累计");
            }
        }

        return score.min(BigDecimal.valueOf(100));
    }
}
