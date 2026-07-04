package com.autorecon.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 签章经办人更新 DTO（所有字段可选）
 */
@Data
public class SealOperatorUpdateDTO {

    private String operatorName;
    private String phone;
    private String idNo;
    private String allowedSealTypes;
    private BigDecimal amountLimit;
    private Integer requireApproval;
    private Integer verifyMethod;
}
