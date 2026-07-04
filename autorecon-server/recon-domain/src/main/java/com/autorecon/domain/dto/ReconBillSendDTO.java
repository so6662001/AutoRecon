package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 对账单发送 DTO
 */
@Data
public class ReconBillSendDTO {

    @NotNull(message = "对账单ID不能为空")
    private Long billId;

    @NotEmpty(message = "通知渠道不能为空")
    private List<String> notifyChannels; // sms, email, wechat
}
