package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 单项企业数据授权
 */
@Data
public class AuthorizationItemDTO {

    @NotBlank(message = "授权类型不能为空")
    private String authorizationType;

    @NotNull(message = "是否授权不能为空")
    private Boolean authorized;
}
