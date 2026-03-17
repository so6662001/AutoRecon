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
 * 付款抵扣明细
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("payment_allocation")
public class PaymentAllocation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long paymentId;
    private Long billId;
    private Long billItemId;
    private String sourceDocNo;
    private BigDecimal allocatedAmount;
    private Integer allocationType;
    private Long allocatedBy;
    private LocalDateTime allocatedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
