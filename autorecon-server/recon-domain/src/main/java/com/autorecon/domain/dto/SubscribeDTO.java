package com.autorecon.domain.dto;

import lombok.Data;

/**
 * 订阅 DTO
 */
@Data
public class SubscribeDTO {

    private Integer planType;
    private Integer billingCycle;
    private String referralCode;
}
