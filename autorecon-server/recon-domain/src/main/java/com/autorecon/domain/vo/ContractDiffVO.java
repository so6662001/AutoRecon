package com.autorecon.domain.vo;

import lombok.Data;

/**
 * 合同差异 VO
 */
@Data
public class ContractDiffVO {

    private String contractNo;
    private Integer totalItems;
    private Integer matchedCount;
    private Integer diffCount;
}
