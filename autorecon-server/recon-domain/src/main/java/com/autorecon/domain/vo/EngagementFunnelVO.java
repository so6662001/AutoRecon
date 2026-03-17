package com.autorecon.domain.vo;

import lombok.Data;

/**
 * 参与漏斗 VO
 */
@Data
public class EngagementFunnelVO {

    private Integer totalSent;
    private Integer totalOpened;
    private Integer totalConfirmed;
    private Integer totalRegistered;
    private Integer totalDataSubmit;
    private Integer totalSealInit;
}
