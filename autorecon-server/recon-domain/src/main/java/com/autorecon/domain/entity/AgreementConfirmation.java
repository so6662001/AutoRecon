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
 * 用户协议确认记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agreement_confirmation")
public class AgreementConfirmation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long enterpriseId;
    private Long agreementVersionId;
    private Integer agreementType;
    private String versionNo;
    private LocalDateTime confirmedAt;
    /** 确认方式: 1-登录弹窗确认 2-注册时确认 3-免注册确认 */
    private Integer confirmMethod;
    private String ipAddress;
    private String userAgent;
    private String contentSnapshotUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
