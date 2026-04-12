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

import java.time.LocalDateTime;

/**
 * 仓库
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("warehouse")
public class Warehouse {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long enterpriseId;
    private String warehouseName;
    private String address;
    private String contactName;
    private String contactPhone;
    /** Legacy column; prefer {@link #defaultDeliveryMode} when present in DB. */
    private Integer deliveryMode;
    @TableField("default_delivery_mode")
    private Integer defaultDeliveryMode;
    @TableField("backup_delivery_mode")
    private Integer backupDeliveryMode;
    @TableField("has_wms")
    private Integer hasWms;
    private String wmsConfig;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
