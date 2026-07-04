package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 访客确认 DTO
 */
@Data
public class GuestConfirmDTO {

    @NotBlank(message = "访问令牌不能为空")
    private String token;

    private String phoneVerifyCode;

    @NotNull(message = "确认状态不能为空")
    private Boolean confirmed;

    private String disputeMessage;
}
