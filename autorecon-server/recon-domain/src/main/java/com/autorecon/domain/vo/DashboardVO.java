package com.autorecon.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 仪表盘 VO
 */
@Data
public class DashboardVO {

    private Integer pendingCount;
    private Integer disputedCount;
    private Integer toSignCount;
    private Integer collectingCount;
    private BigDecimal monthlyCompletionRate;
    private BigDecimal totalReceivable;
    private BigDecimal totalOverdue;
    private List<TodoItem> recentTodos;

    /**
     * 待办项
     */
    @Data
    public static class TodoItem {
        private String billNo;
        private String buyerName;
        private String action;
        private String time;
    }
}
