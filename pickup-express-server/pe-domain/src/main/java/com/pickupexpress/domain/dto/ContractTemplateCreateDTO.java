package com.pickupexpress.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 合同模板创建 DTO
 */
@Data
public class ContractTemplateCreateDTO {

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotNull(message = "合同类型不能为空")
    private Integer contractType;

    private Integer templateSource;
    private String templateContent;
    private String templateFileUrl;
    private String fieldMapping;
    private Integer hasOriginField;
    private Integer platformTermsPriority;
    private Integer isDefault;
}
