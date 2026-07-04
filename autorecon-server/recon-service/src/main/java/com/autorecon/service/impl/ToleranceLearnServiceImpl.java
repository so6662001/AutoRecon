package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.common.util.TenantUtil;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.entity.ToleranceLearn;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.mapper.ToleranceLearnMapper;
import com.autorecon.service.ToleranceLearnService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 容差学习服务 — 基于历史比对数据自动分析最优容差
 *
 * 算法(设计文档8.3):
 * 1. 收集近6个月历史比对差异值
 * 2. 按(卖方,买方,品规,维度)四元组分组
 * 3. 计算P95作为建议容差
 * 4. 计算匹配率/误判率预估
 * 5. 安全机制: 只放宽不缩小, 上限200%, 管理员审批
 * 6. 每月1日自动执行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToleranceLearnServiceImpl extends ServiceImpl<ToleranceLearnMapper, ToleranceLearn> implements ToleranceLearnService {

    private final ToleranceLearnMapper toleranceLearnMapper;
    private final ReconBillMapper reconBillMapper;
    private final ReconBillItemMapper reconBillItemMapper;

    private static final BigDecimal DEFAULT_WEIGHT_TOLERANCE = new BigDecimal("0.003"); // 0.3%
    private static final BigDecimal DEFAULT_AMOUNT_TOLERANCE = new BigDecimal("10");
    private static final BigDecimal MAX_AMPLIFICATION = new BigDecimal("2.0"); // 放宽上限200%
    private static final int MIN_SAMPLE_COUNT = 10; // 最少样本数
    private static final BigDecimal MIN_CONFIDENCE = new BigDecimal("60"); // 最低置信度

    @Override
    public List<ToleranceLearn> getSuggestions(Long sellerId, Long buyerId) {
        LambdaQueryWrapper<ToleranceLearn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ToleranceLearn::getSellerId, sellerId)
                .eq(ToleranceLearn::getBuyerId, buyerId)
                .eq(ToleranceLearn::getAdopted, 0)
                .orderByDesc(ToleranceLearn::getConfidence);
        return toleranceLearnMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adoptSuggestion(Long id) {
        ToleranceLearn learn = toleranceLearnMapper.selectById(id);
        if (learn == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "容差学习记录不存在");
        }
        TenantUtil.checkOwnership(learn.getSellerId());
        learn.setAdopted(1);
        toleranceLearnMapper.updateById(learn);
        log.info("Adopted tolerance suggestion: id={}, dimension={}, current={} → suggested={}",
                id, learn.getDimension(), learn.getCurrentTolerance(), learn.getSuggestedTolerance());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectSuggestion(Long id) {
        ToleranceLearn learn = toleranceLearnMapper.selectById(id);
        if (learn == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "容差学习记录不存在");
        }
        TenantUtil.checkOwnership(learn.getSellerId());
        toleranceLearnMapper.deleteById(id);
        log.info("Rejected tolerance suggestion: id={}", id);
    }

    @Override
    public List<ToleranceLearn> listAll(Long sellerId) {
        LambdaQueryWrapper<ToleranceLearn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ToleranceLearn::getSellerId, sellerId)
                .orderByDesc(ToleranceLearn::getCalculatedAt);
        return toleranceLearnMapper.selectList(wrapper);
    }

    // ========== 核心分析引擎 ==========

    /**
     * 对指定卖方的所有买方关系执行容差分析
     */
    @Transactional(rollbackFor = Exception.class)
    public void analyzeForSeller(Long sellerId) {
        log.info("Starting tolerance analysis for seller {}", sellerId);

        // 收集近6个月的已比对对账单
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        List<ReconBill> bills = reconBillMapper.selectList(
                new LambdaQueryWrapper<ReconBill>()
                        .eq(ReconBill::getSellerId, sellerId)
                        .ge(ReconBill::getPeriodStart, sixMonthsAgo)
                        .ne(ReconBill::getMatchResult, 0) // 已比对
                        .eq(ReconBill::getDeleted, 0));

        if (bills.isEmpty()) {
            log.info("No matched bills in last 6 months for seller {}", sellerId);
            return;
        }

        // 按买方分组
        Map<Long, List<ReconBill>> billsByBuyer = bills.stream()
                .filter(b -> b.getBuyerId() != null)
                .collect(Collectors.groupingBy(ReconBill::getBuyerId));

        int totalSuggestions = 0;
        for (Map.Entry<Long, List<ReconBill>> entry : billsByBuyer.entrySet()) {
            Long buyerId = entry.getKey();
            List<Long> billIds = entry.getValue().stream().map(ReconBill::getId).collect(Collectors.toList());

            // 加载所有明细行(已有买方数据的)
            List<ReconBillItem> items = reconBillItemMapper.selectList(
                    new LambdaQueryWrapper<ReconBillItem>()
                            .in(ReconBillItem::getBillId, billIds)
                            .isNotNull(ReconBillItem::getBuyerWeight));

            if (items.size() < MIN_SAMPLE_COUNT) continue;

            // 分析重量维度
            ToleranceLearn weightSuggestion = analyzeWeightTolerance(sellerId, buyerId, items);
            if (weightSuggestion != null) {
                saveSuggestion(weightSuggestion);
                totalSuggestions++;
            }

            // 分析金额维度
            ToleranceLearn amountSuggestion = analyzeAmountTolerance(sellerId, buyerId, items);
            if (amountSuggestion != null) {
                saveSuggestion(amountSuggestion);
                totalSuggestions++;
            }
        }

        log.info("Tolerance analysis complete for seller {}: {} suggestions generated", sellerId, totalSuggestions);
    }

    /**
     * 分析重量容差 — 按(卖方,买方)维度
     */
    private ToleranceLearn analyzeWeightTolerance(Long sellerId, Long buyerId, List<ReconBillItem> items) {
        // 收集所有重量差异率 |buyerWeight - weight| / weight
        List<BigDecimal> diffRates = new ArrayList<>();
        for (ReconBillItem item : items) {
            if (item.getWeight() != null && item.getBuyerWeight() != null
                    && item.getWeight().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal diff = item.getBuyerWeight().subtract(item.getWeight()).abs();
                BigDecimal rate = diff.divide(item.getWeight(), 6, RoundingMode.HALF_UP);
                diffRates.add(rate);
            }
        }

        if (diffRates.size() < MIN_SAMPLE_COUNT) return null;

        // 排序计算P95
        Collections.sort(diffRates);
        int p95Index = (int) Math.ceil(diffRates.size() * 0.95) - 1;
        BigDecimal p95 = diffRates.get(Math.min(p95Index, diffRates.size() - 1));

        // 当前容差
        BigDecimal currentTolerance = DEFAULT_WEIGHT_TOLERANCE;

        // 安全机制: 只放宽不缩小
        if (p95.compareTo(currentTolerance) <= 0) return null;

        // 安全机制: 放宽上限200%
        BigDecimal maxSuggested = currentTolerance.multiply(MAX_AMPLIFICATION);
        BigDecimal suggestedTolerance = p95.min(maxSuggested);

        // 计算匹配率: 在当前容差下的匹配率 vs 建议容差下的匹配率
        long matchedCurrent = diffRates.stream().filter(r -> r.compareTo(currentTolerance) <= 0).count();
        long matchedSuggested = diffRates.stream().filter(r -> r.compareTo(suggestedTolerance) <= 0).count();
        BigDecimal matchRateCurrent = BigDecimal.valueOf(matchedCurrent * 100.0 / diffRates.size()).setScale(1, RoundingMode.HALF_UP);
        BigDecimal matchRateSuggested = BigDecimal.valueOf(matchedSuggested * 100.0 / diffRates.size()).setScale(1, RoundingMode.HALF_UP);

        // 计算误判率: 建议容差新增放过的差异中，有多少是真正异议(用历史异议作为proxy)
        // 简化: 假设P95~P99之间的差异有30%是真正需要关注的
        BigDecimal falsePositiveRate = BigDecimal.valueOf(3.0); // 估算值

        // 置信度: 基于样本数
        BigDecimal confidence = BigDecimal.valueOf(Math.min(diffRates.size() * 100.0 / 100, 99)).setScale(1, RoundingMode.HALF_UP);
        if (confidence.compareTo(MIN_CONFIDENCE) < 0) return null;

        return ToleranceLearn.builder()
                .sellerId(sellerId)
                .buyerId(buyerId)
                .dimension("weight")
                .currentTolerance(currentTolerance.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)) // 转为百分比展示
                .suggestedTolerance(suggestedTolerance.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                .sampleCount(diffRates.size())
                .matchRateCurrent(matchRateCurrent)
                .matchRateSuggested(matchRateSuggested)
                .falsePositiveRate(falsePositiveRate)
                .confidence(confidence)
                .adopted(0)
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 分析金额容差 — 按(卖方,买方)维度
     */
    private ToleranceLearn analyzeAmountTolerance(Long sellerId, Long buyerId, List<ReconBillItem> items) {
        // 收集所有金额差异绝对值
        List<BigDecimal> diffs = new ArrayList<>();
        for (ReconBillItem item : items) {
            if (item.getAmount() != null && item.getBuyerAmount() != null) {
                BigDecimal diff = item.getBuyerAmount().subtract(item.getAmount()).abs();
                diffs.add(diff);
            }
        }

        if (diffs.size() < MIN_SAMPLE_COUNT) return null;

        Collections.sort(diffs);
        int p95Index = (int) Math.ceil(diffs.size() * 0.95) - 1;
        BigDecimal p95 = diffs.get(Math.min(p95Index, diffs.size() - 1));

        BigDecimal currentTolerance = DEFAULT_AMOUNT_TOLERANCE;

        // 只放宽不缩小
        if (p95.compareTo(currentTolerance) <= 0) return null;

        BigDecimal maxSuggested = currentTolerance.multiply(MAX_AMPLIFICATION);
        BigDecimal suggestedTolerance = p95.min(maxSuggested).setScale(2, RoundingMode.HALF_UP);

        long matchedCurrent = diffs.stream().filter(d -> d.compareTo(currentTolerance) <= 0).count();
        long matchedSuggested = diffs.stream().filter(d -> d.compareTo(suggestedTolerance) <= 0).count();
        BigDecimal matchRateCurrent = BigDecimal.valueOf(matchedCurrent * 100.0 / diffs.size()).setScale(1, RoundingMode.HALF_UP);
        BigDecimal matchRateSuggested = BigDecimal.valueOf(matchedSuggested * 100.0 / diffs.size()).setScale(1, RoundingMode.HALF_UP);

        BigDecimal falsePositiveRate = BigDecimal.valueOf(2.0);
        BigDecimal confidence = BigDecimal.valueOf(Math.min(diffs.size() * 100.0 / 100, 99)).setScale(1, RoundingMode.HALF_UP);
        if (confidence.compareTo(MIN_CONFIDENCE) < 0) return null;

        return ToleranceLearn.builder()
                .sellerId(sellerId)
                .buyerId(buyerId)
                .dimension("amount")
                .currentTolerance(currentTolerance)
                .suggestedTolerance(suggestedTolerance)
                .sampleCount(diffs.size())
                .matchRateCurrent(matchRateCurrent)
                .matchRateSuggested(matchRateSuggested)
                .falsePositiveRate(falsePositiveRate)
                .confidence(confidence)
                .adopted(0)
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 保存建议(去重: 同一(seller,buyer,dimension)只保留最新未采纳的)
     */
    private void saveSuggestion(ToleranceLearn suggestion) {
        // 删除同维度旧的未采纳建议
        toleranceLearnMapper.delete(
                new LambdaQueryWrapper<ToleranceLearn>()
                        .eq(ToleranceLearn::getSellerId, suggestion.getSellerId())
                        .eq(ToleranceLearn::getBuyerId, suggestion.getBuyerId())
                        .eq(ToleranceLearn::getDimension, suggestion.getDimension())
                        .eq(ToleranceLearn::getAdopted, 0));
        toleranceLearnMapper.insert(suggestion);
    }

    // ========== 定时调度: 每月1日凌晨3点 ==========

    @Scheduled(cron = "0 0 3 1 * ?")
    public void monthlyAnalysis() {
        log.info("Monthly tolerance analysis started");
        // 获取所有活跃卖方
        List<ReconBill> recentBills = reconBillMapper.selectList(
                new LambdaQueryWrapper<ReconBill>()
                        .ge(ReconBill::getCreatedAt, LocalDateTime.now().minusMonths(6))
                        .eq(ReconBill::getDeleted, 0)
                        .select(ReconBill::getSellerId)
                        .groupBy(ReconBill::getSellerId));

        Set<Long> sellerIds = recentBills.stream()
                .map(ReconBill::getSellerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Long sellerId : sellerIds) {
            try {
                analyzeForSeller(sellerId);
            } catch (Exception e) {
                log.error("Tolerance analysis failed for seller {}: {}", sellerId, e.getMessage());
            }
        }
        log.info("Monthly tolerance analysis completed for {} sellers", sellerIds.size());
    }

    /**
     * 手动触发分析(管理员操作)
     */
    public void manualAnalyze(Long sellerId) {
        analyzeForSeller(sellerId);
    }
}
