package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 异议创建 DTO
 */
@Data
public class DisputeCreateDTO {

    @NotNull(message = "对账单ID不能为空")
    private Long billId;

    private Long billItemId;

    @NotNull(message = "异议类型不能为空")
    private Integer disputeType;

    @NotBlank(message = "异议描述不能为空")
    private String description;
}
