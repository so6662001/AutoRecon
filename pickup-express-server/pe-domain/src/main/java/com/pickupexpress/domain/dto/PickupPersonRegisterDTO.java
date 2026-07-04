package com.pickupexpress.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 授权提货人注册 DTO
 */
@Data
public class PickupPersonRegisterDTO {

    @NotNull(message = "买方ID不能为空")
    private Long buyerId;

    private Long contractId;

    @NotBlank(message = "提货人姓名不能为空")
    private String personName;

    private String idCardNo;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String vehiclePlate;

    private BigDecimal maxPickupWeight;

    /**
     * 1-客户自填 2-销售 3-仓库
     */
    @NotNull(message = "注册来源不能为空")
    private Integer registeredBy;
}
