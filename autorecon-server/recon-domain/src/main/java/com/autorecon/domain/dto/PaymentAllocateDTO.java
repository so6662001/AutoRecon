package com.autorecon.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 付款分配 DTO
 */
@Data
public class PaymentAllocateDTO {

    @NotNull(message = "付款ID不能为空")
    private Long paymentId;

    @NotEmpty(message = "分配明细不能为空")
    @Valid
    private List<AllocationItem> allocations;

    /**
     * 分配项
     */
    @Data
    public static class AllocationItem {
        @NotNull(message = "对账单ID不能为空")
        private Long billId;
        private Long billItemId;
        private String sourceDocNo;
        @NotNull(message = "分配金额不能为空")
        private BigDecimal amount;
    }
}
