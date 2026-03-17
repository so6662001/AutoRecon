package com.pickupexpress.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 派车请求 DTO
 */
@Data
public class DispatchRequestDTO {

    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    @NotNull(message = "派车模式不能为空")
    private Integer dispatchMode;

    private String vehiclePlate;
    private String driverName;
    private String driverPhone;
    private Long carrierId;
    private String carrierName;
    private LocalDateTime expectedArrivalAt;
}
