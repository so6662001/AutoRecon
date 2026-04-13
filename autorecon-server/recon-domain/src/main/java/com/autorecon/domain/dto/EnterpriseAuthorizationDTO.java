package com.autorecon.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量更新企业数据授权
 */
@Data
public class EnterpriseAuthorizationDTO {

    @NotNull(message = "企业ID不能为空")
    private Long enterpriseId;

    @NotEmpty(message = "授权项不能为空")
    @Valid
    private List<AuthorizationItemDTO> authorizations;
}
