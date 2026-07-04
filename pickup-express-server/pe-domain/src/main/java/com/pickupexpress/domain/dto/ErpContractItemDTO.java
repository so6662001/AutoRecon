package com.pickupexpress.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * ERP 同步 — 合同明细行
 */
@Data
public class ErpContractItemDTO {

    private String productName;
    private String spec;
    private String material;
    private String origin;
    private BigDecimal quantity;
    private BigDecimal weight;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
