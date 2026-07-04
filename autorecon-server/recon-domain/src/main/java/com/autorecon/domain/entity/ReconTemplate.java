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
 * 对账单模板
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("recon_template")
public class ReconTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long enterpriseId;
    private String templateName;
    private Integer templateType;
    private String headerConfig;
    private String columnConfig;
    private String footerConfig;
    private String styleConfig;
    private String groupBy;
    private String sortBy;
    private Integer isDefault;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
