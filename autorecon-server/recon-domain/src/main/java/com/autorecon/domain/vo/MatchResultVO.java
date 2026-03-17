package com.autorecon.domain.vo;

import com.autorecon.domain.entity.ReconBillItem;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 匹配结果 VO
 */
@Data
public class MatchResultVO {

    private Long billId;
    private Integer totalItems;
    private Integer matchedCount;
    private Integer diffCount;
    private Integer sellerExtraCount;
    private Integer buyerExtraCount;
    private BigDecimal matchRate;
    private List<ReconBillItem> items;
}
