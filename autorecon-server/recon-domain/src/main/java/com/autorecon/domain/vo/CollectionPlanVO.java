package com.autorecon.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 催收计划 VO
 */
@Data
public class CollectionPlanVO {

    private Long id;
    private Long billId;
    private Long sellerId;
    private Long buyerId;
    private BigDecimal receivableAmount;
    private BigDecimal collectedAmount;
    private BigDecimal remainingAmount;
    private LocalDate dueDate;
    private String strategyLevel;
    private Integer status;
    private Integer currentStage;
    private LocalDate nextActionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 扩展字段
    private String sellerName;
    private String buyerName;
    private String billNo;
    private Integer overdueDays;
}
