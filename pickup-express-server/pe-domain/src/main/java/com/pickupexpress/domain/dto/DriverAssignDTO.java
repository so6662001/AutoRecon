package com.pickupexpress.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 驾驶员分配 DTO
 */
@Data
public class DriverAssignDTO {

    @NotNull(message = "提货单ID不能为空")
    private Long pickupOrderId;

    @NotBlank(message = "驾驶员姓名不能为空")
    private String driverName;

    @NotBlank(message = "驾驶员手机号不能为空")
    private String driverPhone;

    private String vehiclePlate;

    private Long carrierId;

    private String carrierName;
}
