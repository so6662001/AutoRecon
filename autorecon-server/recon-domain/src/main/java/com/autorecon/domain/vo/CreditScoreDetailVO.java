package com.autorecon.domain.vo;

import com.autorecon.domain.entity.Payment;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 信用评分详情 VO
 */
@Data
public class CreditScoreDetailVO {

    private Long id;
    private Long enterpriseId;
    private Long sellerId;
    private BigDecimal creditScore;
    private String scoreLevel;
    private BigDecimal avgPaymentDays;
    private BigDecimal overdueRate;
    private BigDecimal disputeRate;
    private BigDecimal totalTradeAmount;
    private BigDecimal totalOverdueAmount;
    private String scoreFactors;
    private LocalDateTime lastCalculatedAt;
    private Integer trend;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 扩展字段
    private String buyerName;
    private List<Payment> recentPayments;
}
