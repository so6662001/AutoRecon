package com.autorecon.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 对账单详情 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReconBillDetailVO extends ReconBillVO {

    private List<ReconBillItemVO> items;
    private List<PaymentVO> payments;
    private List<DisputeVO> disputes;
}
