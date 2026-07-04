package com.autorecon.domain.vo;

import lombok.Data;

/**
 * 参与漏斗 VO（字段名与前端漏斗组件约定一致）
 */
@Data
public class EngagementFunnelVO {

    private Integer sent;
    private Integer opened;
    private Integer confirmed;
    private Integer registered;
    private Integer dataSubmit;
    private Integer sealInit;
}
