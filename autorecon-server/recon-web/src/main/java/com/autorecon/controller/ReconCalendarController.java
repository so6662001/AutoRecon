package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.CalendarEventDTO;
import com.autorecon.domain.entity.ReconCalendar;
import com.autorecon.service.ReconCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对账日历 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/calendar")
@Tag(name = "对账日历")
@RequiredArgsConstructor
public class ReconCalendarController {

    private final ReconCalendarService reconCalendarService;

    @GetMapping("/")
    @Operation(summary = "获取指定月份日历事件")
    public R<List<ReconCalendar>> getEvents(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        List<ReconCalendar> events = reconCalendarService.getEvents(enterpriseId, year, month);
        return R.ok(events);
    }

    @GetMapping("/upcoming")
    @Operation(summary = "获取即将到来的事件")
    public R<List<ReconCalendar>> getUpcoming(
            @RequestParam(defaultValue = "7") Integer days) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        List<ReconCalendar> events = reconCalendarService.getUpcoming(enterpriseId, days);
        return R.ok(events);
    }

    @PostMapping("/")
    @Operation(summary = "创建日历事件")
    public R<Long> createEvent(@RequestBody CalendarEventDTO dto) {
        Long eventId = reconCalendarService.createEvent(dto);
        return R.ok(eventId);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新事件状态")
    public R<Void> updateEventStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        reconCalendarService.updateEventStatus(id, status);
        return R.ok();
    }
}
