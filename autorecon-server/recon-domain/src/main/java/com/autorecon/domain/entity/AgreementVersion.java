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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 协议版本
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agreement_version")
public class AgreementVersion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 协议类型: 1-用户服务协议 2-隐私保护政策 */
    private Integer agreementType;
    private String versionNo;
    private String title;
    private String content;
    private String contentUrl;
    private String summary;
    private LocalDate effectiveDate;
    private Long publishedBy;
    private LocalDateTime publishedAt;
    /** 状态: 0-草稿 1-已发布 2-已废弃 */
    private Integer status;
    /** 是否要求用户重新确认: 0-否 1-是 */
    private Integer requireReconfirm;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
