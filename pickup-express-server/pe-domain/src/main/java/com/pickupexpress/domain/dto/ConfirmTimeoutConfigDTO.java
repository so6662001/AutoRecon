package com.pickupexpress.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 确认时效配置 DTO
 */
@Data
public class ConfirmTimeoutConfigDTO {

    private Long id;

    @NotNull(message = "企业ID不能为空")
    private Long enterpriseId;

    /**
     * 买方ID，null表示企业级配置
     */
    private Long buyerId;

    private Integer contractType;

    /**
     * 场景类型，如 dispatch_confirm, settlement_confirm
     */
    @NotNull(message = "配置类型不能为空")
    private String configType;

    private Integer hours;

    private Integer days;
}
