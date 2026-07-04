package com.autorecon.domain.dto;

import lombok.Data;

/**
 * 企业认证提交 DTO
 */
@Data
public class EnterpriseAuthDTO {

    private String companyName;
    private String unifiedCreditCode;
    private String legalPersonName;
    private String legalPersonIdNo;
    private String legalPersonPhone;
    private String businessLicenseUrl;
    private String legalPersonIdFrontUrl;
    private String legalPersonIdBackUrl;
    private String authorizationLetterUrl;
}
