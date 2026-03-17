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
 * 发货确认
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("delivery_confirm")
public class DeliveryConfirm {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long pickupOrderId;
    private String operatorId;
    private String operatorName;
    private String signatureUrl;
    private Integer totalLifts;
    private Integer totalPieces;
    private BigDecimal totalWeight;
    private LocalDateTime confirmedAt;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
