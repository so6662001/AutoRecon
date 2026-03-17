package com.autorecon.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 在线提交买方数据 DTO
 */
@Data
public class OnlineSubmitDTO {

    @NotNull(message = "对账单ID不能为空")
    private Long billId;

    @Valid
    private List<ReconBillItemDTO> buyerItems;
}
