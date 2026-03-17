package com.autorecon.domain.dto;

import lombok.Data;

/**
 * 买方数据配置 DTO
 */
@Data
public class BuyerDataConfigDTO {

    private Integer submitMode;
    private String excelMappingConfig;
    private Integer defaultConfirmMode;
    private Integer ocrEnabled;
    private Integer mobileEnabled;
}
