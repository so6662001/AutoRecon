package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 企业创建/更新 DTO
 */
@Data
public class EnterpriseCreateDTO {

    @NotBlank(message = "企业名称不能为空")
    private String companyName;
    private String unifiedCreditCode;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private Integer enterpriseType;
    private String logoUrl;
}
