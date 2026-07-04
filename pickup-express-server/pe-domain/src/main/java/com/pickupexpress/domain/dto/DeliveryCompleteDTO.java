package com.pickupexpress.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发货完成 DTO
 */
@Data
public class DeliveryCompleteDTO {

    @NotNull(message = "提货单ID不能为空")
    private Long pickupOrderId;

    private String operatorId;
    private String operatorName;
    private String signatureUrl;
    private String remark;
}
