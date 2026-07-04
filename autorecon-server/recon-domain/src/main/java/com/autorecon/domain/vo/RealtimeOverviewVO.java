package com.autorecon.domain.vo;

import lombok.Data;

@Data
public class RealtimeOverviewVO {

    private Integer todayPv;
    private Integer todayUv;
    private Integer currentOnline;
    private Integer todayEvents;
    private Integer avgDuration;
}
