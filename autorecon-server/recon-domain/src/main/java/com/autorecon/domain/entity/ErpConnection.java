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
 * ERP连接配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("erp_connection")
public class ErpConnection {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long enterpriseId;
    private String connectionName;
    private Integer connectionType;
    private String baseUrl;
    private Integer authType;
    private String authConfig;
    private String fieldMapping;
    private Integer pullStrategy;
    private String cronExpression;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
