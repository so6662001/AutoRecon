package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员响应授权变更通知
 */
@Data
public class AuthorizationChangeRespondDTO {

    @NotNull(message = "是否接受不能为空")
    private Boolean accept;

    private String detail;
}
