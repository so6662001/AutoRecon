package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 印章创建 DTO
 */
@Data
public class SealCreateDTO {

    @NotBlank(message = "印章名称不能为空")
    private String sealName;

    @NotNull(message = "印章类型不能为空")
    private Integer sealType;

    @NotNull(message = "印章来源不能为空")
    private Integer sealSource;

    private String sealImageUrl;
}
