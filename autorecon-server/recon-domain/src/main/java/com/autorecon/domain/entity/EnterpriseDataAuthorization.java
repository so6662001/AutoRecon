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
 * 企业数据授权
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("enterprise_data_authorization")
public class EnterpriseDataAuthorization {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String authorizationType;
    private Integer authorized;
    private Long authorizedBy;
    private LocalDateTime authorizedAt;
    /** 授权方式: 1-注册签章 2-管理员确认 3-变更确认 */
    private Integer authorizationMethod;
    private String ipAddress;
    private String userAgent;
    private Integer revoked;
    private LocalDateTime revokedAt;
    private Long revokedBy;
    private String snapshotUrl;
    private String versionNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
