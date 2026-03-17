package com.autorecon.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同汇总 VO
 */
@Data
public class ContractSummaryVO {

    private String contractNo;
    private String contractName;
    private Integer itemCount;
    private BigDecimal totalWeight;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal unpaidAmount;
}
