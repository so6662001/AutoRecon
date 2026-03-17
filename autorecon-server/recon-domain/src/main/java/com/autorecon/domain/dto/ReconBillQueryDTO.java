package com.autorecon.domain.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 对账单查询 DTO
 */
@Data
public class ReconBillQueryDTO {

    private Long sellerId;
    private Long buyerId;
    private String status;
    private String billNo;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String contractNo;
    private String keyword;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
