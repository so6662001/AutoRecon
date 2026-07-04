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
 * 付款记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("payment")
public class Payment {

    @TableId(type = IdType.ASSIGN_ID)
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
