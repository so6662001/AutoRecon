package com.autorecon.domain.dto;

import lombok.Data;

/**
 * 自动对账计划创建 DTO
 */
@Data
public class AutoReconPlanCreateDTO {

    private String planName;
    private Long buyerId;  // nullable = all
    private Integer frequency;
    private Integer executionDay;
    private String executionTime;
    private Long templateId;
    private Integer periodType;
    private Integer autoSend;
    private Integer includePayment;
}
