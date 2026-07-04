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

import java.time.LocalDateTime;

/**
 * 异议记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dispute")
public class Dispute {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long billId;
    private Long billItemId;
    private Integer disputeType;
    private String description;
    private Long raisedBy;
    private Integer raisedBySide;
    private Integer status;
    private LocalDateTime resolvedAt;
    private String resolution;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
