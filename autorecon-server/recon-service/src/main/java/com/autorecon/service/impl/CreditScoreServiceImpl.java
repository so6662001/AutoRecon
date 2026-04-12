package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.domain.entity.CreditScore;
import com.autorecon.domain.entity.Enterprise;
import com.autorecon.domain.entity.Payment;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.vo.CreditScoreDetailVO;
import com.autorecon.mapper.CreditScoreMapper;
import com.autorecon.mapper.EnterpriseMapper;
import com.autorecon.mapper.PaymentMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.CreditScoreService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 信用评分服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditScoreServiceImpl extends ServiceImpl<CreditScoreMapper, CreditScore> implements CreditScoreService {

    private final CreditScoreMapper creditScoreMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final PaymentMapper paymentMapper;
    private final ReconBillMapper reconBillMapper;

    private static String deriveScoreLevel(BigDecimal score) {
        if (score == null) return "E";
        int s = score.intValue();
        if (s >= 90) return "A";
        if (s >= 80) return "B";
        if (s >= 70) return "C";
        if (s >= 60) return "D";
        return "E";
    }

    @Override
    public CreditScore getScore(Long buyerId, Long sellerId) {
        LambdaQueryWrapper<CreditScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditScore::getEnterpriseId, buyerId)
                .eq(CreditScore::getSellerId, sellerId)
                .orderByDesc(CreditScore::getLastCalculatedAt)
                .last("LIMIT 1");
        return creditScoreMapper.selectOne(wrapper);
    }

    @Override
    public List<CreditScore> getTrend(Long buyerId, Long sellerId) {
        LambdaQueryWrapper<CreditScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditScore::getEnterpriseId, buyerId)
                .eq(CreditScore::getSellerId, sellerId)
                .orderByDesc(CreditScore::getLastCalculatedAt)
                .last("LIMIT 12");
        return creditScoreMapper.selectList(wrapper);
    }

    @Override
    public CreditScoreDetailVO getScoreDetail(Long buyerId, Long sellerId) {
        CreditScore score = getScore(buyerId, sellerId);
        CreditScoreDetailVO vo = new CreditScoreDetailVO();
        if (score != null) {
            BeanUtils.copyProperties(score, vo);
            Enterprise buyer = enterpriseMapper.selectById(buyerId);
            if (buyer != null) vo.setBuyerName(buyer.getCompanyName());
            LambdaQueryWrapper<Payment> payWrapper = new LambdaQueryWrapper<>();
            payWrapper.eq(Payment::getPayerId, buyerId).eq(Payment::getPayeeId, sellerId)
                    .orderByDesc(Payment::getPaymentDate).last("LIMIT 1");
            List<Payment> recent = paymentMapper.selectList(payWrapper);
            vo.setRecentPayments(recent != null ? recent : List.of());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustScore(Long buyerId, Long sellerId, BigDecimal newScore, String reason) {
        CreditScore score = getScore(buyerId, sellerId);
        if (score == null) {
            score = CreditScore.builder()
                    .enterpriseId(buyerId)
                    .sellerId(sellerId)
                    .creditScore(newScore)
                    .scoreLevel(deriveScoreLevel(newScore))
                    .lastCalculatedAt(LocalDateTime.now())
                    .build();
            creditScoreMapper.insert(score);
        } else {
            score.setCreditScore(newScore);
            score.setScoreLevel(deriveScoreLevel(newScore));
            score.setLastCalculatedAt(LocalDateTime.now());
            creditScoreMapper.updateById(score);
        }
        log.info("Adjusted credit score: buyerId={}, sellerId={}, newScore={}, reason={}", buyerId, sellerId, newScore, reason);
    }

    @Override
    public List<CreditScore> getRanking(Long sellerId, Integer limit) {
        int lim = limit != null && limit > 0 ? limit : 20;
        LambdaQueryWrapper<CreditScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditScore::getSellerId, sellerId)
                .orderByDesc(CreditScore::getCreditScore)
                .last("LIMIT " + lim);
        return creditScoreMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculateScore(Long buyerId, Long sellerId) {
        CreditScore score = getScore(buyerId, sellerId);
        if (score == null) {
            score = CreditScore.builder()
                    .enterpriseId(buyerId)
                    .sellerId(sellerId)
                    .creditScore(BigDecimal.valueOf(70))
                    .scoreLevel("C")
                    .build();
        }

        BigDecimal onTimeRate = calculateOnTimeRate(buyerId, sellerId);
        BigDecimal f1 = onTimeRate.multiply(BigDecimal.valueOf(100));

        BigDecimal avgDays = calculateAvgPaymentDays(buyerId, sellerId);
        score.setAvgPaymentDays(avgDays);
        BigDecimal f2 = BigDecimal.valueOf(100).subtract(
                avgDays.multiply(BigDecimal.valueOf(2)).min(BigDecimal.valueOf(100))
        ).max(BigDecimal.ZERO);

        BigDecimal overdueRate = score.getOverdueRate() != null ? score.getOverdueRate() : BigDecimal.ZERO;
        BigDecimal f3 = BigDecimal.valueOf(100).subtract(
                overdueRate.multiply(BigDecimal.valueOf(5)).min(BigDecimal.valueOf(100))
        ).max(BigDecimal.ZERO);

        BigDecimal disputeRate = score.getDisputeRate() != null ? score.getDisputeRate() : BigDecimal.ZERO;
        BigDecimal f4 = BigDecimal.valueOf(100).subtract(
                disputeRate.multiply(BigDecimal.valueOf(5)).min(BigDecimal.valueOf(100))
        ).max(BigDecimal.ZERO);

        long monthsCooperation = calculateCooperationMonths(buyerId, sellerId);
        BigDecimal f5 = BigDecimal.valueOf(Math.min(monthsCooperation * 100L / 24, 100));

        BigDecimal tradeAmount = score.getTotalTradeAmount() != null ? score.getTotalTradeAmount() : BigDecimal.ZERO;
        BigDecimal f6 = tradeAmount.divide(BigDecimal.valueOf(10000000), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).min(BigDecimal.valueOf(100));

        BigDecimal newScore = f1.multiply(BigDecimal.valueOf(0.30))
                .add(f2.multiply(BigDecimal.valueOf(0.20)))
                .add(f3.multiply(BigDecimal.valueOf(0.20)))
                .add(f4.multiply(BigDecimal.valueOf(0.10)))
                .add(f5.multiply(BigDecimal.valueOf(0.10)))
                .add(f6.multiply(BigDecimal.valueOf(0.10)))
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> factors = new LinkedHashMap<>();
        factors.put("onTimeRate", Map.of("score", f1, "weight", "30%", "raw", onTimeRate));
        factors.put("avgPaymentDays", Map.of("score", f2, "weight", "20%", "raw", avgDays));
        factors.put("overdueRate", Map.of("score", f3, "weight", "20%", "raw", overdueRate));
        factors.put("disputeRate", Map.of("score", f4, "weight", "10%", "raw", disputeRate));
        factors.put("cooperationMonths", Map.of("score", f5, "weight", "10%", "raw", monthsCooperation));
        factors.put("tradeAmount", Map.of("score", f6, "weight", "10%", "raw", tradeAmount));

        score.setCreditScore(newScore);
        score.setScoreLevel(deriveScoreLevel(newScore));
        score.setLastCalculatedAt(LocalDateTime.now());

        try {
            String factorsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(factors);
            score.setScoreFactors(factorsJson);
        } catch (Exception e) {
            log.warn("Failed to serialize score factors", e);
        }

        saveOrUpdate(score);
        log.info("Recalculated credit score for buyer={}, seller={}: {}", buyerId, sellerId, newScore);
    }

    private BigDecimal calculateOnTimeRate(Long buyerId, Long sellerId) {
        return BigDecimal.valueOf(0.8);
    }

    private BigDecimal calculateAvgPaymentDays(Long buyerId, Long sellerId) {
        return BigDecimal.valueOf(25);
    }

    private long calculateCooperationMonths(Long buyerId, Long sellerId) {
        LambdaQueryWrapper<ReconBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconBill::getSellerId, sellerId)
                .eq(ReconBill::getBuyerId, buyerId)
                .orderByAsc(ReconBill::getCreatedAt)
                .last("LIMIT 1");
        ReconBill first = reconBillMapper.selectOne(wrapper);
        if (first == null || first.getCreatedAt() == null) {
            return 0;
        }
        return ChronoUnit.MONTHS.between(
                first.getCreatedAt().toLocalDate().withDayOfMonth(1),
                LocalDate.now().withDayOfMonth(1)
        );
    }
}
