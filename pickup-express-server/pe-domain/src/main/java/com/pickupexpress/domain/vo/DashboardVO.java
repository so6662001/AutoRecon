package com.pickupexpress.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 仪表盘 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVO {

    /**
     * 合同数量统计（按状态）
     */
    private Long contractReadyCount;
    private Long contractPendingSignCount;
    private Long contractSignedCount;
    private Long contractPickingCount;
    private Long contractPickedCount;
    private Long contractSettledCount;

    /**
     * 提货单数量统计（按发货状态）
     */
    private Long pickupNotStartedCount;
    private Long pickupInProgressCount;
    private Long pickupCompletedCount;

    /**
     * 应收金额汇总
     */
    private BigDecimal totalReceivableAmount;

    /**
     * 最近进度事件
     */
    private List<ProgressEventSummaryVO> recentEvents;
}
