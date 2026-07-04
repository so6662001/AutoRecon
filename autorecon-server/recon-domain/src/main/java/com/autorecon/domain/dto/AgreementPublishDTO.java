package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 发布协议版本（管理员）
 */
@Data
public class AgreementPublishDTO {

    @NotNull(message = "协议类型不能为空")
    private Integer agreementType;

    @NotBlank(message = "版本号不能为空")
    private String versionNo;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String content;
    private String summary;

    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    private Integer requireReconfirm;
}
