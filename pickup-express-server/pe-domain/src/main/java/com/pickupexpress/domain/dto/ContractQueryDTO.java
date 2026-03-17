package com.pickupexpress.domain.dto;

import lombok.Data;

/**
 * 合同查询 DTO
 */
@Data
public class ContractQueryDTO {

    private Long sellerId;
    private Long buyerId;
    private String contractNo;
    private Integer contractType;
    private Integer status;
    private String keyword;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
