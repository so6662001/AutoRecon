package com.pickupexpress.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 结算生成 DTO
 */
@Data
public class SettlementGenerateDTO {

    @NotNull(message = "提货单ID不能为空")
    private Long pickupOrderId;
}
