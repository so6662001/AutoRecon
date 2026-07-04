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
 * 催收执行记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("collection_log")
public class CollectionLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long planId;
    private Long billId;
    private Integer actionType;
    private Integer actionStage;
    private String content;
    private LocalDateTime executedAt;
    private String executedBy;
    private String result;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
