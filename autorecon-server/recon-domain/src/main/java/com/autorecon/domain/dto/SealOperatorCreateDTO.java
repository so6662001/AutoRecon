package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 签章经办人创建 DTO
 */
@Data
public class SealOperatorCreateDTO {

    @NotBlank(message = "经办人姓名不能为空")
    private String operatorName;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String idNo;

    private String allowedSealTypes;

    private BigDecimal amountLimit;

    private Integer requireApproval;

    private Integer verifyMethod;
}
