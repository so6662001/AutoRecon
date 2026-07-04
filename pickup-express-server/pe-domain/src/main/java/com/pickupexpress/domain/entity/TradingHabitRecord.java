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
 * 交易习惯记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("trading_habit_record")
public class TradingHabitRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long buyerId;
    private String driverName;
    private String driverPhone;
    private String vehiclePlate;
    private Integer totalPickups;
    private BigDecimal totalWeight;
    private BigDecimal totalAmount;
    private Integer paidPickups;
    private Integer deniedPickups;
    private LocalDateTime lastPickupAt;
    private LocalDateTime firstPickupAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
