package com.pickupexpress.domain.vo;

import com.pickupexpress.domain.entity.LiftRecord;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发货进度 VO
 */
@Data
public class DeliveryProgressVO {

    private Long pickupOrderId;
    private Integer totalLifts;
    private Integer completedLifts;
    private BigDecimal totalWeight;
    private BigDecimal currentWeight;
    private List<LiftRecord> lifts;
    private Integer status;
}
