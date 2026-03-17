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
 * 发货吊装记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("lift_record")
public class LiftRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long pickupOrderId;
    private Integer liftSeq;
    private String productName;
    private String spec;
    private String material;
    private String heatNo;
    private String batchNo;
    private Integer pieces;
    private BigDecimal theoreticalWeight;
    private BigDecimal actualWeight;
    private String operatorId;
    private String operatorName;
    private String wmsRecordId;
    private Integer dataSource;
    private String photoUrl;
    private LocalDateTime uploadedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
