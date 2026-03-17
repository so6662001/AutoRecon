package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发票关联 DTO
 */
@Data
public class InvoiceLinkDTO {

    @NotNull(message = "对账单ID不能为空")
    private Long billId;

    private Long billItemId;

    @NotNull(message = "发票ID不能为空")
    private Long invoiceId;

    private BigDecimal linkAmount;
}
