package com.pickupexpress.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 买方免注册访问 — 提货详情
 */
@Data
public class GuestPickupVO {

    private String pickupNo;
    private String contractNo;
    private String sellerName;
    private String buyerName;
    private List<GuestPickupItemVO> items;
    private BigDecimal totalWeight;
    private BigDecimal totalAmount;
    private String warehouseName;
    private String driverName;
    private String vehiclePlate;
    private LocalDateTime completedAt;
    private List<String> photos;
    private BigDecimal settlementAmount;

    @Data
    public static class GuestPickupItemVO {
        private String productName;
        private String spec;
        private Integer pieces;
        private BigDecimal weight;
    }
}
