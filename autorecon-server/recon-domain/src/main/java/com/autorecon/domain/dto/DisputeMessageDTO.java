package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 异议消息 DTO
 */
@Data
public class DisputeMessageDTO {

    @NotNull(message = "异议ID不能为空")
    private Long disputeId;

    @NotNull(message = "消息类型不能为空")
    private Integer messageType;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    private String attachmentUrl;
}
