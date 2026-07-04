package com.pickupexpress.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 进度事件摘要 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressEventSummaryVO {

    private Long id;
    private Long pickupOrderId;
    private Long contractId;
    private String eventType;
    private String eventTitle;
    private String eventDetail;
    private String operator;
    private LocalDateTime createdAt;
}
