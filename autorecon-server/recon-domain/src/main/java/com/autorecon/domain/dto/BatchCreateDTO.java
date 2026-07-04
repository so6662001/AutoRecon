package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 批量创建对账单 DTO
 */
@Data
public class BatchCreateDTO {

    @NotEmpty(message = "买方列表不能为空")
    private List<Long> buyerIds;

    @NotNull(message = "账期开始不能为空")
    private LocalDate periodStart;

    @NotNull(message = "账期结束不能为空")
    private LocalDate periodEnd;

    @NotNull(message = "模板ID不能为空")
    private Long templateId;
}
