package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 模板创建 DTO
 */
@Data
public class TemplateCreateDTO {

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotNull(message = "模板类型不能为空")
    private Integer templateType;

    private String headerConfig;
    private String columnConfig;
    private String footerConfig;
    private String styleConfig;
    private String groupBy;
    private String sortBy;
    private Integer isDefault;
}
