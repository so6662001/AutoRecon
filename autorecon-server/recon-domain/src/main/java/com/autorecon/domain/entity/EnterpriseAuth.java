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
 * 企业认证信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("enterprise_auth")
public class EnterpriseAuth {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long enterpriseId;
    private String companyName;
    private String unifiedCreditCode;
    private String legalPersonName;
    private String legalPersonIdNo;
    private String legalPersonPhone;
    private String businessLicenseUrl;
    private String legalPersonIdFrontUrl;
    private String legalPersonIdBackUrl;
    private String authorizationLetterUrl;
    private String thirdPartyOrgId;
    private Integer authStatus;
    private String authFailReason;
    private LocalDateTime authPassedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
