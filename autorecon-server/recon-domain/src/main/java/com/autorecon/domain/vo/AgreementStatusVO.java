package com.autorecon.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 当前用户协议确认状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementStatusVO {

    private Boolean needConfirm;

    @Builder.Default
    private List<AgreementVersionVO> unconfirmedAgreements = new ArrayList<>();
}
