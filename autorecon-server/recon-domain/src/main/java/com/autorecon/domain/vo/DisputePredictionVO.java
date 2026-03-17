package com.autorecon.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 异议预测 VO
 */
@Data
public class DisputePredictionVO {

    private Long billId;
    private BigDecimal overallScore;
    private String riskLevel;
    private List<PredictionItem> highRiskItems;

    @Data
    public static class PredictionItem {
        private Long itemId;
        private Integer lineNo;
        private String productName;
        private String spec;
        private BigDecimal riskScore;
        private String riskReason;
    }
}
