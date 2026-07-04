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
 * 企业账户
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("enterprise")
public class Enterprise {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String companyName;
    private String unifiedCreditCode;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private Integer enterpriseType;
    private Integer status;
    private String logoUrl;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
