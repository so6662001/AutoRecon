package com.pickupexpress.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 事后补录创建 DTO
 */
@Data
public class SupplementCreateDTO {

    @NotNull(message = "提货单ID不能为空")
    private Long pickupOrderId;

    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    /**
     * 补录数据 JSON
     */
    private String supplementData;

    /**
     * 凭证URL，多个用逗号分隔
     */
    private String documentUrls;
}
