package com.autorecon.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 对账单创建 DTO
 */
@Data
public class ReconBillCreateDTO {

    @NotNull(message = "买方ID不能为空")
    private Long buyerId;

    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    @NotNull(message = "账期开始日期不能为空")
    private LocalDate periodStart;

    @NotNull(message = "账期结束日期不能为空")
    private LocalDate periodEnd;

    private Integer matchMode;
    private Boolean autoSend;
    private Boolean includePayment;
    private Integer paymentAllocStrategy;
    private String remark;

    @Valid
    private List<ReconBillItemDTO> items;
}
