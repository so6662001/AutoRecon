package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 付款创建 DTO
 */
@Data
public class PaymentCreateDTO {

    @NotNull(message = "付款方ID不能为空")
    private Long payerId;

    @NotNull(message = "收款方ID不能为空")
    private Long payeeId;

    @NotNull(message = "付款日期不能为空")
    private LocalDate paymentDate;

    @NotNull(message = "付款金额不能为空")
    private BigDecimal paymentAmount;

    @NotNull(message = "付款方式不能为空")
    private Integer paymentMethod;

    private String bankSerialNo;
    private String remark;
    /** 来源：业务可扩展，默认由服务端处理 */
    private Integer source;
}
