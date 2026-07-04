package com.pickupexpress.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 吊装上传 DTO
 */
@Data
public class LiftUploadDTO {

    @NotNull(message = "提货单ID不能为空")
    private Long pickupOrderId;

    private Integer liftSeq;
    private String productName;
    private String spec;
    private String material;
    private String heatNo;
    private String batchNo;
    private Integer pieces;
    private BigDecimal theoreticalWeight;
    private BigDecimal actualWeight;
    private String operatorId;
    private String operatorName;
    private Integer dataSource;
}
