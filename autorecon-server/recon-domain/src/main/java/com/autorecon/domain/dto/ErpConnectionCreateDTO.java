package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ERP连接创建/更新 DTO
 */
@Data
public class ErpConnectionCreateDTO {

    @NotBlank(message = "连接名称不能为空")
    private String connectionName;
    private Integer connectionType;
    private String baseUrl;
    private Integer authType;
    private String authConfig;
    private String fieldMapping;
    private Integer pullStrategy;
    private String cronExpression;
}
