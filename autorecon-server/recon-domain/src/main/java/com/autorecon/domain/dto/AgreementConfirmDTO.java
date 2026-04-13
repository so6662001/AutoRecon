package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户确认协议
 */
@Data
public class AgreementConfirmDTO {

    @NotNull(message = "协议版本ID不能为空")
    private Long agreementVersionId;

    @NotNull(message = "协议类型不能为空")
    private Integer agreementType;
}
