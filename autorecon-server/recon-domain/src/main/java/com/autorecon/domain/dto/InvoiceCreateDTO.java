package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 发票创建 DTO
 */
@Data
public class InvoiceCreateDTO {

    @NotBlank(message = "发票号码不能为空")
    private String invoiceNo;

    private String invoiceCode;

    @NotNull(message = "发票类型不能为空")
    private Integer invoiceType;

    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    @NotNull(message = "开票日期不能为空")
    private LocalDate invoiceDate;

    private String buyerName;
    private String sellerName;
}
