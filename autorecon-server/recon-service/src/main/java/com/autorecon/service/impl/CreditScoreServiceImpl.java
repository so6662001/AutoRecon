package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.domain.entity.CreditScore;
import com.autorecon.domain.entity.Enterprise;
import com.autorecon.domain.entity.Payment;
import com.autorecon.domain.vo.CreditScoreDetailVO;
import com.autorecon.mapper.CreditScoreMapper;
import com.autorecon.mapper.EnterpriseMapper;
import com.autorecon.mapper.PaymentMapper;
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
import java.time.LocalDateTime;
import java.util.List;

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
        BigDecimal overdueRate = score != null && score.getOverdueRate() != null ? score.getOverdueRate() : BigDecimal.ZERO;
        BigDecimal disputeRate = score != null && score.getDisputeRate() != null ? score.getDisputeRate() : BigDecimal.ZERO;

        // Simple formula: base 100 - overdue penalty - dispute penalty
        BigDecimal base = BigDecimal.valueOf(100);
        BigDecimal overduePenalty = overdueRate.multiply(BigDecimal.valueOf(30)).min(BigDecimal.valueOf(30));
        BigDecimal disputePenalty = disputeRate.multiply(BigDecimal.valueOf(20)).min(BigDecimal.valueOf(20));
        BigDecimal newScore = base.subtract(overduePenalty).subtract(disputePenalty).max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));

        if (score == null) {
            score = CreditScore.builder()
                    .enterpriseId(buyerId)
                    .sellerId(sellerId)
                    .creditScore(newScore.setScale(2, RoundingMode.HALF_UP))
                    .scoreLevel(deriveScoreLevel(newScore))
                    .overdueRate(overdueRate)
                    .disputeRate(disputeRate)
                    .lastCalculatedAt(LocalDateTime.now())
                    .build();
            creditScoreMapper.insert(score);
        } else {
            score.setCreditScore(newScore.setScale(2, RoundingMode.HALF_UP));
            score.setScoreLevel(deriveScoreLevel(newScore));
            score.setLastCalculatedAt(LocalDateTime.now());
            creditScoreMapper.updateById(score);
        }
        log.info("Recalculated credit score: buyerId={}, sellerId={}, score={}", buyerId, sellerId, newScore);
    }
}
