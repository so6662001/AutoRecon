package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.enums.BillStatusEnum;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.vo.MatchResultVO;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.MatchEngineService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 匹配引擎服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchEngineServiceImpl implements MatchEngineService {

    private static final int MATCHED = 1;
    private static final int DIFF = 2;
    private static final int SELLER_EXTRA = 3;
    private static final int UNMATCHED = 0;

    /** 重量容差 0.3% */
    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.003");
    /** 金额容差 10 */
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("10");

    private final ReconBillMapper reconBillMapper;
    private final ReconBillItemMapper reconBillItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MatchResultVO executeMatch(Long billId) {
        ReconBill bill = reconBillMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }

        List<ReconBillItem> items = getBillItems(billId);
        if (items == null) {
            items = new ArrayList<>();
        }

        int matchedCount = 0;
        int diffCount = 0;
        int sellerExtraCount = 0;
        int buyerExtraCount = 0;

        for (ReconBillItem item : items) {
            int status = computeMatchStatus(item);
            item.setMatchStatus(status);
            if (item.getBuyerWeight() != null && item.getWeight() != null
                    && item.getBuyerWeight().subtract(item.getWeight()).abs().compareTo(BigDecimal.ZERO) > 0) {
                item.setWeightDiffCause(attributeWeightDiff(item));
            }
            reconBillItemMapper.updateById(item);

            switch (status) {
                case MATCHED -> matchedCount++;
                case DIFF -> diffCount++;
                case SELLER_EXTRA -> sellerExtraCount++;
                default -> buyerExtraCount++;
            }
        }

        int totalItems = items.size();
        if (totalItems == 0) {
            bill.setMatchResult(0); // 未比对
        } else if (matchedCount == totalItems) {
            bill.setMatchResult(1); // 一致
        } else {
            bill.setMatchResult(2); // 有差异
        }

        if (bill.getMatchResult() == 1) {
            if (BillStatusEnum.PENDING.getCode().equals(bill.getStatus())) {
                bill.setStatus(BillStatusEnum.TO_SIGN.getCode());
                log.info("Bill {} auto-confirmed by match engine (all matched)", billId);
            }
        } else if (bill.getMatchResult() == 2) {
            if (BillStatusEnum.PENDING.getCode().equals(bill.getStatus())) {
                bill.setStatus(BillStatusEnum.DISPUTED.getCode());
                log.info("Bill {} auto-disputed by match engine (differences found)", billId);
            }
        }
        reconBillMapper.updateById(bill);

        return buildMatchResultVO(billId, items, items.size(), matchedCount, diffCount, sellerExtraCount, buyerExtraCount);
    }

    @Override
    public MatchResultVO getMatchResult(Long billId) {
        ReconBill bill = reconBillMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }

        List<ReconBillItem> items = getBillItems(billId);
        if (items == null) {
            items = new ArrayList<>();
        }

        int matchedCount = 0;
        int diffCount = 0;
        int sellerExtraCount = 0;
        int buyerExtraCount = 0;

        for (ReconBillItem item : items) {
            Integer status = item.getMatchStatus();
            if (status == null) status = UNMATCHED;
            switch (status) {
                case MATCHED -> matchedCount++;
                case DIFF -> diffCount++;
                case SELLER_EXTRA -> sellerExtraCount++;
                default -> buyerExtraCount++;
            }
        }

        return buildMatchResultVO(billId, items, items.size(), matchedCount, diffCount, sellerExtraCount, buyerExtraCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rematch(Long billId) {
        ReconBill bill = reconBillMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }

        List<ReconBillItem> items = getBillItems(billId);
        if (items != null) {
            for (ReconBillItem item : items) {
                item.setMatchStatus(UNMATCHED);
                reconBillItemMapper.updateById(item);
            }
        }

        executeMatch(billId);
    }

    @Override
    public List<ReconBillItem> getDiffItems(Long billId) {
        List<ReconBillItem> items = getBillItems(billId);
        if (items == null) {
            return new ArrayList<>();
        }
        return items.stream()
                .filter(item -> item.getMatchStatus() != null && item.getMatchStatus() != MATCHED)
                .toList();
    }

    private List<ReconBillItem> getBillItems(Long billId) {
        LambdaQueryWrapper<ReconBillItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconBillItem::getBillId, billId).orderByAsc(ReconBillItem::getLineNo);
        return reconBillItemMapper.selectList(wrapper);
    }

    private int computeMatchStatus(ReconBillItem item) {
        boolean hasBuyerQty = item.getBuyerQuantity() != null;
        boolean hasBuyerWeight = item.getBuyerWeight() != null;
        boolean hasBuyerAmount = item.getBuyerAmount() != null;

        if (!hasBuyerQty && !hasBuyerWeight && !hasBuyerAmount) {
            return SELLER_EXTRA;
        }

        boolean weightOk = true;
        if (hasBuyerWeight && item.getWeight() != null) {
            BigDecimal diff = item.getBuyerWeight().subtract(item.getWeight()).abs();
            BigDecimal threshold = item.getWeight().multiply(WEIGHT_TOLERANCE);
            weightOk = diff.compareTo(threshold) <= 0;
        }

        boolean amountOk = true;
        if (hasBuyerAmount && item.getTotalAmount() != null) {
            BigDecimal diff = item.getBuyerAmount().subtract(item.getTotalAmount()).abs();
            amountOk = diff.compareTo(AMOUNT_TOLERANCE) <= 0;
        }

        boolean quantityOk = true;
        if (hasBuyerQty && item.getQuantity() != null) {
            BigDecimal diff = item.getBuyerQuantity().subtract(item.getQuantity()).abs();
            quantityOk = diff.compareTo(BigDecimal.ZERO) == 0;
        }

        return (weightOk && amountOk && quantityOk) ? MATCHED : DIFF;
    }

    /**
     * 重量差异归因引擎 (设计文档8.4)
     *
     * 7条归因规则优先级: R1→R6→R5→R2→R4→R3→R7
     * 输出: 归因类别 + 原因说明 + 置信度 + 建议处理方式
     */
    private String attributeWeightDiff(ReconBillItem item) {
        if (item.getWeight() == null || item.getBuyerWeight() == null) {
            return null;
        }

        BigDecimal diff = item.getBuyerWeight().subtract(item.getWeight());
        BigDecimal absDiff = diff.abs();
        BigDecimal diffRate = absDiff.divide(item.getWeight(), 4, RoundingMode.HALF_UP);
        String pct = diffRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP) + "%";

        // R1: 磅差 (≤0.3%, 检查是否存在系统性偏差)
        if (diffRate.compareTo(BigDecimal.valueOf(0.003)) <= 0) {
            boolean systematic = checkSystematicBias(item);
            if (systematic) {
                return "[R2·系统偏差] 偏差" + pct + "虽在容差内，但连续多次同方向偏差，疑似磅秤校准差异 | 置信度:85% | 建议:双方校准磅秤后重新过磅";
            }
            return "[R1·正常磅差] 正常过磅误差(偏差" + pct + ")，在允许范围内 | 置信度:95% | 建议:无需处理";
        }

        // R6: 数量差异 (差异约等于整件重量的倍数)
        if (item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal avgPieceWeight = item.getWeight().divide(item.getQuantity(), 4, RoundingMode.HALF_UP);
            if (avgPieceWeight.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pieceDiff = absDiff.divide(avgPieceWeight, 1, RoundingMode.HALF_UP);
                BigDecimal remainder = absDiff.remainder(avgPieceWeight);
                BigDecimal remainderRate = remainder.divide(avgPieceWeight, 2, RoundingMode.HALF_UP);
                // 差异接近整数倍件重(余数<30%件重)
                if (pieceDiff.compareTo(BigDecimal.ONE) >= 0
                        && pieceDiff.compareTo(BigDecimal.valueOf(5)) <= 0
                        && remainderRate.compareTo(BigDecimal.valueOf(0.3)) < 0) {
                    int pieces = pieceDiff.setScale(0, RoundingMode.HALF_UP).intValue();
                    String action = diff.compareTo(BigDecimal.ZERO) < 0 ? "漏发" : "多发";
                    return "[R6·数量差异] 疑似" + action + pieces + "件(偏差" + absDiff.setScale(2, RoundingMode.HALF_UP)
                            + "吨，单件均重" + avgPieceWeight.setScale(3, RoundingMode.HALF_UP) + "吨) | 置信度:88% | 建议:核查发货件数清点记录";
                }
            }
        }

        // R5: 理论重量偏差 (≤1%)
        if (diffRate.compareTo(BigDecimal.valueOf(0.01)) <= 0) {
            return "[R5·理重偏差] 实际重量与理论重量偏差属正常公差范围(偏差" + pct + ") | 置信度:90% | 建议:无需处理，属正常生产偏差";
        }

        // R2: 系统性偏差 (≤2%, 或检测到连续同方向)
        boolean systematic = checkSystematicBias(item);
        if (diffRate.compareTo(BigDecimal.valueOf(0.02)) <= 0) {
            if (systematic) {
                return "[R2·系统偏差] 连续多次同方向偏差(偏差" + pct + ")，磅秤可能存在系统误差 | 置信度:82% | 建议:安排双方联合校准磅秤";
            }
            return "[R2·系统偏差] 疑似磅秤校准差异(偏差" + pct + ") | 置信度:75% | 建议:建议双方校准磅秤";
        }

        // R4: 含水量差异 (雨季6-9月, 且买方重量>卖方)
        int month = java.time.LocalDate.now().getMonthValue();
        if (month >= 6 && month <= 9 && diff.compareTo(BigDecimal.ZERO) > 0 && diffRate.compareTo(BigDecimal.valueOf(0.03)) <= 0) {
            return "[R4·含水量] 当前为雨季("+month+"月)，买方过磅重量偏大(偏差" + pct + ")，可能因含水量增加 | 置信度:70% | 建议:参考天气情况判断，必要时烘干后复磅";
        }

        // R3: 运输损耗 (买方 < 卖方, ≤3%)
        if (diff.compareTo(BigDecimal.ZERO) < 0 && diffRate.compareTo(BigDecimal.valueOf(0.03)) <= 0) {
            return "[R3·运输损耗] 运输途中正常损耗(损耗率" + pct + ") | 置信度:80% | 建议:属正常运输损耗范围，按合同约定容差处理";
        }

        // R7: 异常差异
        return "[R7·异常差异] 差异异常(偏差" + absDiff.setScale(2, RoundingMode.HALF_UP) + "吨/" + pct
                + ")，超出所有已知归因规则 | 置信度:N/A | 建议:需人工核查，检查是否存在发错货、漏装、串货等情况";
    }

    /**
     * R2辅助: 检测系统性偏差 — 查看同一买方+品规的近期比对是否连续同方向
     */
    private boolean checkSystematicBias(ReconBillItem item) {
        if (item.getBillId() == null) return false;
        try {
            // 查询同一对账单内同品规的其他明细，看差异方向是否一致
            List<ReconBillItem> sameSpecItems = reconBillItemMapper.selectList(
                    new LambdaQueryWrapper<ReconBillItem>()
                            .eq(ReconBillItem::getBillId, item.getBillId())
                            .eq(ReconBillItem::getProductName, item.getProductName())
                            .eq(ReconBillItem::getSpec, item.getSpec())
                            .isNotNull(ReconBillItem::getBuyerWeight)
                            .ne(ReconBillItem::getId, item.getId()));

            if (sameSpecItems.size() < 2) return false;

            // 检查是否所有差异都是同一方向
            boolean allPositive = true, allNegative = true;
            for (ReconBillItem other : sameSpecItems) {
                if (other.getWeight() == null || other.getBuyerWeight() == null) continue;
                BigDecimal otherDiff = other.getBuyerWeight().subtract(other.getWeight());
                if (otherDiff.compareTo(BigDecimal.ZERO) >= 0) allNegative = false;
                if (otherDiff.compareTo(BigDecimal.ZERO) <= 0) allPositive = false;
            }
            // 当前item的差异方向也要一致
            BigDecimal currentDiff = item.getBuyerWeight().subtract(item.getWeight());
            if (currentDiff.compareTo(BigDecimal.ZERO) > 0 && allPositive) return true;
            if (currentDiff.compareTo(BigDecimal.ZERO) < 0 && allNegative) return true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private MatchResultVO buildMatchResultVO(Long billId, List<ReconBillItem> items,
                                             int totalItems, int matchedCount, int diffCount,
                                             int sellerExtraCount, int buyerExtraCount) {
        MatchResultVO vo = new MatchResultVO();
        vo.setBillId(billId);
        vo.setTotalItems(totalItems);
        vo.setMatchedCount(matchedCount);
        vo.setDiffCount(diffCount);
        vo.setSellerExtraCount(sellerExtraCount);
        vo.setBuyerExtraCount(buyerExtraCount);
        vo.setItems(items);

        BigDecimal rate = totalItems == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(matchedCount).divide(BigDecimal.valueOf(totalItems), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        vo.setMatchRate(rate);
        return vo;
    }
}
