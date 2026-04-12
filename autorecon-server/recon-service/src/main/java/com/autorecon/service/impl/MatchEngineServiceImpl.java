package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.domain.entity.ReconBill;
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
