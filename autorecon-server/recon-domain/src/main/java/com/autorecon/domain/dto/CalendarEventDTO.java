package com.autorecon.domain.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 日历事件创建 DTO
 */
@Data
public class CalendarEventDTO {

    private Integer eventType;
    private LocalDate eventDate;
    private String eventTitle;
    private Long relatedBillId;
    private Long relatedBuyerId;
}
