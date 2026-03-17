package com.autorecon.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发票关联
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("invoice_link")
public class InvoiceLink {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long billId;
    private Long billItemId;
    private Long invoiceId;
    private String invoiceNo;
    private String invoiceCode;
    private Integer invoiceType;
    private BigDecimal invoiceAmount;
    private BigDecimal taxAmount;
    private LocalDate invoiceDate;
    private BigDecimal linkAmount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
