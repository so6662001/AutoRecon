package com.autorecon.service.impl;

import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.vo.ContractDiffVO;
import com.autorecon.domain.vo.ContractSummaryVO;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.ContractReconService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 合同对账服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractReconServiceImpl implements ContractReconService {

    private static final int MATCHED = 1;

    private final ReconBillMapper reconBillMapper;
    private final ReconBillItemMapper reconBillItemMapper;

    @Override
    public List<ContractSummaryVO> listContracts(Long sellerId, Long buyerId, LocalDate periodStart, LocalDate periodEnd) {
        List<Long> billIds = getBillIds(sellerId, buyerId, periodStart, periodEnd);
        if (billIds.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<ReconBillItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ReconBillItem::getBillId, billIds);
        List<ReconBillItem> items = reconBillItemMapper.selectList(wrapper);

        Map<String, ContractSummaryVO> map = new HashMap<>();
        for (ReconBillItem item : items) {
            String contractNo = item.getContractNo() != null ? item.getContractNo() : "";
            ContractSummaryVO vo = map.computeIfAbsent(contractNo, k -> {
                ContractSummaryVO v = new ContractSummaryVO();
                v.setContractNo(contractNo);
                v.setContractName(item.getContractName());
                v.setItemCount(0);
                v.setTotalWeight(BigDecimal.ZERO);
                v.setTotalAmount(BigDecimal.ZERO);
                v.setPaidAmount(BigDecimal.ZERO);
                v.setUnpaidAmount(BigDecimal.ZERO);
                return v;
            });
            vo.setItemCount(vo.getItemCount() + 1);
            vo.setTotalWeight(vo.getTotalWeight().add(nullToZero(item.getWeight())));
            vo.setTotalAmount(vo.getTotalAmount().add(nullToZero(item.getTotalAmount())));
            vo.setPaidAmount(vo.getPaidAmount().add(nullToZero(item.getPaidAmount())));
            vo.setUnpaidAmount(vo.getUnpaidAmount().add(nullToZero(item.getUnpaidAmount())));
        }
        return new ArrayList<>(map.values());
    }

    @Override
    public List<ReconBillItem> getContractItems(String contractNo) {
        if (!StringUtils.hasText(contractNo)) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<ReconBillItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconBillItem::getContractNo, contractNo).orderByAsc(ReconBillItem::getBillId).orderByAsc(ReconBillItem::getLineNo);
        return reconBillItemMapper.selectList(wrapper);
    }

    @Override
    public ContractSummaryVO getContractSummary(String contractNo) {
        List<ReconBillItem> items = getContractItems(contractNo);
        ContractSummaryVO vo = new ContractSummaryVO();
        vo.setContractNo(contractNo);
        vo.setItemCount(items.size());
        vo.setTotalWeight(BigDecimal.ZERO);
        vo.setTotalAmount(BigDecimal.ZERO);
        vo.setPaidAmount(BigDecimal.ZERO);
        vo.setUnpaidAmount(BigDecimal.ZERO);

        for (ReconBillItem item : items) {
            if (item.getContractName() != null) vo.setContractName(item.getContractName());
            vo.setTotalWeight(vo.getTotalWeight().add(nullToZero(item.getWeight())));
            vo.setTotalAmount(vo.getTotalAmount().add(nullToZero(item.getTotalAmount())));
            vo.setPaidAmount(vo.getPaidAmount().add(nullToZero(item.getPaidAmount())));
            vo.setUnpaidAmount(vo.getUnpaidAmount().add(nullToZero(item.getUnpaidAmount())));
        }
        return vo;
    }

    @Override
    public Map<String, List<ReconBillItem>> getBillGroupByContract(Long billId) {
        LambdaQueryWrapper<ReconBillItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconBillItem::getBillId, billId).orderByAsc(ReconBillItem::getLineNo);
        List<ReconBillItem> items = reconBillItemMapper.selectList(wrapper);
        return items.stream().collect(Collectors.groupingBy(item -> item.getContractNo() != null ? item.getContractNo() : ""));
    }

    @Override
    public Map<String, ContractDiffVO> getBillContractDiff(Long billId) {
        Map<String, List<ReconBillItem>> group = getBillGroupByContract(billId);
        Map<String, ContractDiffVO> result = new HashMap<>();
        for (Map.Entry<String, List<ReconBillItem>> e : group.entrySet()) {
            ContractDiffVO vo = new ContractDiffVO();
            vo.setContractNo(e.getKey());
            vo.setTotalItems(e.getValue().size());
            long matched = e.getValue().stream().filter(i -> MATCHED == (i.getMatchStatus() != null ? i.getMatchStatus() : 0)).count();
            vo.setMatchedCount((int) matched);
            vo.setDiffCount(e.getValue().size() - (int) matched);
            result.put(e.getKey(), vo);
        }
        return result;
    }

    private List<Long> getBillIds(Long sellerId, Long buyerId, LocalDate periodStart, LocalDate periodEnd) {
        LambdaQueryWrapper<ReconBill> wrapper = new LambdaQueryWrapper<>();
        if (sellerId != null) wrapper.eq(ReconBill::getSellerId, sellerId);
        if (buyerId != null) wrapper.eq(ReconBill::getBuyerId, buyerId);
        if (periodStart != null) wrapper.le(ReconBill::getPeriodStart, periodEnd);
        if (periodEnd != null) wrapper.ge(ReconBill::getPeriodEnd, periodStart);
        List<ReconBill> bills = reconBillMapper.selectList(wrapper);
        return bills.stream().map(ReconBill::getId).toList();
    }

    private BigDecimal nullToZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
