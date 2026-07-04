package com.autorecon.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付款 VO
 */
@Data
public class PaymentVO {

    private Long id;
    private String paymentNo;
    private Long payerId;
    private Long payeeId;
    private LocalDate paymentDate;
    private BigDecimal paymentAmount;
    private Integer paymentMethod;
    private String bankSerialNo;
    private BigDecimal allocatedAmount;
    private BigDecimal unallocatedAmount;
    private Integer source;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 扩展字段
    private String payerName;
    private String payeeName;
}
