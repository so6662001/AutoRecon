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
 * 对账单明细
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("recon_bill_item")
public class ReconBillItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long billId;
    private Integer lineNo;
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
    private Integer matchStatus;
    private BigDecimal buyerQuantity;
    private BigDecimal buyerWeight;
    private BigDecimal buyerAmount;
    private BigDecimal diffQuantity;
    private BigDecimal diffWeight;
    private BigDecimal diffAmount;
    private BigDecimal paidAmount;
    private BigDecimal unpaidAmount;
    private String invoiceNo;
    private Integer invoiceStatus;
    private Integer disputeRiskLevel;
    private String disputeRiskReason;
    private String weightDiffCause;
    private Integer buyerDataSource;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
