package com.autorecon.domain.vo;

import lombok.Data;

@Data
public class PageValueVO {

    private String pagePath;
    private String pageName;
    private String pageModule;
    private Integer pv;
    private Integer uv;
    private Integer avgDuration;
    private Integer bounceCount;
    private Double bounceRate;
    private Double conversionRate;
    private Double valueScore;
    private String valueLevel;
}
