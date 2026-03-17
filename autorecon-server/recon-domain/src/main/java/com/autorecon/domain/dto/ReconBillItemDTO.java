package com.autorecon.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 对账单明细 DTO
 */
@Data
public class ReconBillItemDTO {

    private String contractNo;
    private String contractName;
    private String orderNo;
    private String deliveryNo;
    private Integer sourceDocType;
    private String productName;
    private String spec;
    private String material;
    private String origin;
    private String warehouse;
    private BigDecimal quantity;
    private BigDecimal weight;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private LocalDate deliveryDate;
    private LocalDate settleDate;
}
