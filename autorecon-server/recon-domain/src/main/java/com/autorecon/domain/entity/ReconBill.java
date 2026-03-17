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
 * 对账单 (core table)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("recon_bill")
public class ReconBill {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String billNo;
    private String batchId;
    private Long sellerId;
    private Long buyerId;
    private Long templateId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalAmount;
    private BigDecimal totalQuantity;
    private BigDecimal totalWeight;
    private String currency;
    private BigDecimal prevBalance;
    private BigDecimal currentTradeAmount;
    private BigDecimal currentPaymentAmount;
    private BigDecimal currentBalance;
    private Integer paymentAllocStrategy;
    private String status;
    private Integer matchMode;
    private Integer matchResult;
    private BigDecimal disputePredictionScore;
    private Integer sellerSignStatus;
    private Integer buyerSignStatus;
    private LocalDateTime autoConfirmDeadline;
    private Integer autoConfirmed;
    private String pdfUrl;
    private String signedPdfUrl;
    private Integer sourceType;
    private Long autoReconPlanId;
    private String remark;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
