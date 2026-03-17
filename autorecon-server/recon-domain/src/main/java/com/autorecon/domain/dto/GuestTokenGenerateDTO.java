package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 生成访客令牌 DTO
 */
@Data
public class GuestTokenGenerateDTO {

    @NotNull(message = "对账单ID不能为空")
    private Long billId;

    private String buyerPhone;
}
