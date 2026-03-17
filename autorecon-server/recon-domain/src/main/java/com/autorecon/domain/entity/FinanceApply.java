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
import java.time.LocalDateTime;

/**
 * 融资申请
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("finance_apply")
public class FinanceApply {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String applyNo;
    private Long billId;
    private Long sellerId;
    private Long buyerId;
    private Long factorId;
    private BigDecimal applyAmount;
    private BigDecimal approvedAmount;
    private BigDecimal interestRate;
    private Integer financeTermDays;
    private Integer status;
    private String signedPdfUrl;
    private String invoiceUrls;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
