package com.pickupexpress.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 承运公司创建 DTO
 */
@Data
public class CarrierCreateDTO {

    @NotBlank(message = "承运公司名称不能为空")
    private String name;

    private String contactName;
    private String contactPhone;
}
