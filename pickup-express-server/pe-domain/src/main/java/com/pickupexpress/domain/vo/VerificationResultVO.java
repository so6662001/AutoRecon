package com.pickupexpress.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 确权结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResultVO {

    private Long verificationId;
    private Long pickupOrderId;
    private Boolean isPreRegistered;
    private Integer verificationLevel;
    private Integer verificationResult;
    private String message;
}
