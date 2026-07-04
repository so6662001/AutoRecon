package com.autorecon.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class FunnelDataVO {

    private String funnelName;
    private List<FunnelStep> steps;

    @Data
    public static class FunnelStep {
        private String name;
        private Integer count;
        private Double rate;
    }
}
