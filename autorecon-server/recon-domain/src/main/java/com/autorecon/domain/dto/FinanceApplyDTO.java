package com.autorecon.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 融资申请 DTO
 */
@Data
public class FinanceApplyDTO {

    private Long billId;
    private BigDecimal applyAmount;
    private Long factorId;
    private Integer financeTermDays;
}
