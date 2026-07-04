package com.autorecon.domain.vo;

import lombok.Data;

/**
 * 用量统计 VO
 */
@Data
public class UsageVO {

    private Integer planType;
    private String planName;
    private Integer billCount;
    private Integer billLimit;
    private Integer clientCount;
    private Integer clientLimit;
    private Integer sealUsed;
    private Integer sealRemaining;
    private Long storageUsedMb;
}
