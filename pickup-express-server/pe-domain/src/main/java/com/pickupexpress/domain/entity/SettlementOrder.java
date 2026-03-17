package com.pickupexpress.domain.entity;

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
 * 结算单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("settlement_order")
public class SettlementOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String settlementNo;
    private Long pickupOrderId;
    private Long contractId;
    private String contractNo;
    private Long buyerId;
    private BigDecimal totalWeight;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalWithTax;
    private BigDecimal deductedPrepayment;
    private BigDecimal receivableAmount;
    private String settlementDetail;
    private String pdfUrl;
    private Integer customerViewed;
    private LocalDateTime customerViewedAt;
    private Integer syncedToRecon;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
