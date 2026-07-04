package com.autorecon.service;

import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.vo.ContractDiffVO;
import com.autorecon.domain.vo.ContractSummaryVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 合同对账服务接口
 */
public interface ContractReconService {

    /**
     * 列出合同
     */
    List<ContractSummaryVO> listContracts(Long sellerId, Long buyerId, LocalDate periodStart, LocalDate periodEnd);

    /**
     * 获取合同明细
     */
    List<ReconBillItem> getContractItems(String contractNo);

    /**
     * 获取合同汇总
     */
    ContractSummaryVO getContractSummary(String contractNo);

    /**
     * 按合同分组对账单明细
     */
    Map<String, List<ReconBillItem>> getBillGroupByContract(Long billId);

    /**
     * 获取对账单各合同差异统计
     */
    Map<String, ContractDiffVO> getBillContractDiff(Long billId);
}
