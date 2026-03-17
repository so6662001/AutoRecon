package com.pickupexpress.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 派车确认 DTO
 */
@Data
public class DispatchConfirmDTO {

    @NotNull(message = "提货单ID不能为空")
    private Long pickupOrderId;

    @NotNull(message = "确认状态不能为空")
    private Boolean confirmed;
}
