package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.vo.DisputePredictionVO;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.DisputePredictionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 异议预测服务实现（简单规则占位）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisputePredictionServiceImpl implements DisputePredictionService {

    private final ReconBillMapper reconBillMapper;
    private final ReconBillItemMapper reconBillItemMapper;

    private static final Random RANDOM = new Random();

    @Override
    public DisputePredictionVO predict(Long billId) {
        if (reconBillMapper.selectById(billId) == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }

        LambdaQueryWrapper<ReconBillItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconBillItem::getBillId, billId)
                .orderByAsc(ReconBillItem::getLineNo);
        List<ReconBillItem> items = reconBillItemMapper.selectList(wrapper);

        List<DisputePredictionVO.PredictionItem> highRiskItems = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        int count = 0;

        for (ReconBillItem item : items) {
            BigDecimal riskScore = calculateRiskScore(item);
            totalScore = totalScore.add(riskScore);
            count++;

            if (riskScore.compareTo(BigDecimal.valueOf(40)) >= 0) {
                DisputePredictionVO.PredictionItem pi = new DisputePredictionVO.PredictionItem();
                pi.setItemId(item.getId());
                pi.setLineNo(item.getLineNo());
                pi.setProductName(item.getProductName());
                pi.setSpec(item.getSpec());
                pi.setRiskScore(riskScore);
                pi.setRiskReason(buildRiskReason(item, riskScore));
                highRiskItems.add(pi);
            }
        }

        BigDecimal overallScore = count > 0
                ? totalScore.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String riskLevel;
        if (overallScore.compareTo(BigDecimal.valueOf(70)) > 0) {
            riskLevel = "高";
        } else if (overallScore.compareTo(BigDecimal.valueOf(40)) >= 0) {
            riskLevel = "中";
        } else {
            riskLevel = "低";
        }

        DisputePredictionVO vo = new DisputePredictionVO();
        vo.setBillId(billId);
        vo.setOverallScore(overallScore);
        vo.setRiskLevel(riskLevel);
        vo.setHighRiskItems(highRiskItems);
        return vo;
    }

    private BigDecimal calculateRiskScore(ReconBillItem item) {
        BigDecimal score = BigDecimal.ZERO;

        if (item.getDisputeRiskLevel() != null && item.getDisputeRiskLevel() > 0) {
            score = score.add(BigDecimal.valueOf(item.getDisputeRiskLevel()));
        }
        if (item.getWeightDiffCause() != null && !item.getWeightDiffCause().isEmpty()) {
            score = score.add(BigDecimal.valueOf(30));
        }
        if (item.getDiffAmount() != null && item.getDiffAmount().compareTo(BigDecimal.ZERO) != 0) {
            score = score.add(BigDecimal.valueOf(20));
        }
        score = score.add(BigDecimal.valueOf(RANDOM.nextInt(21)));

        return score.min(BigDecimal.valueOf(100));
    }

    private String buildRiskReason(ReconBillItem item, BigDecimal riskScore) {
        List<String> reasons = new ArrayList<>();
        if (item.getWeightDiffCause() != null && !item.getWeightDiffCause().isEmpty()) {
            reasons.add("历史重量差异");
        }
        if (item.getDiffAmount() != null && item.getDiffAmount().compareTo(BigDecimal.ZERO) != 0) {
            reasons.add("金额差异");
        }
        if (item.getDisputeRiskReason() != null && !item.getDisputeRiskReason().isEmpty()) {
            reasons.add(item.getDisputeRiskReason());
        }
        if (reasons.isEmpty()) {
            reasons.add("综合评估");
        }
        return String.join("; ", reasons);
    }
}
