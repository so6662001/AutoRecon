package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 催收计划创建 DTO
 */
@Data
public class CollectionPlanCreateDTO {

    @NotNull(message = "对账单ID不能为空")
    private Long billId;

    @NotNull(message = "卖方ID不能为空")
    private Long sellerId;

    @NotNull(message = "买方ID不能为空")
    private Long buyerId;

    @NotNull(message = "应收金额不能为空")
    private BigDecimal receivableAmount;

    @NotNull(message = "到期日不能为空")
    private LocalDate dueDate;

    private String strategyLevel;
}
