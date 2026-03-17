package com.pickupexpress.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 仓库创建 DTO
 */
@Data
public class WarehouseCreateDTO {

    @NotBlank(message = "仓库名称不能为空")
    private String warehouseName;

    private String warehouseCode;
    private String address;
    private String contactName;
    private String contactPhone;
    private BigDecimal gpsLat;
    private BigDecimal gpsLng;
    private Integer defaultDeliveryMode;
    private Integer hasWms;
}
