package com.pickupexpress.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 照片上传 DTO
 */
@Data
public class PhotoUploadDTO {

    @NotNull(message = "提货单ID不能为空")
    private Long pickupOrderId;

    @NotNull(message = "照片类型不能为空")
    private Integer photoType;

    private BigDecimal gpsLat;
    private BigDecimal gpsLng;
}
