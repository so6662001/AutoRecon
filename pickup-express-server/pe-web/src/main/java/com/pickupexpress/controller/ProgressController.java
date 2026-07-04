package com.pickupexpress.controller;

import com.pickupexpress.common.result.R;
import com.pickupexpress.domain.entity.ProgressEvent;
import com.pickupexpress.service.ProgressEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "进度事件")
@RestController
@RequestMapping("/api/v1/evidence/progress")
@RequiredArgsConstructor
@Slf4j
public class ProgressController {

    private final ProgressEventService progressEventService;

    @Operation(summary = "获取提货单时间线")
    @GetMapping("/pickup/{pickupOrderId}")
    public R<List<ProgressEvent>> getPickupTimeline(@PathVariable Long pickupOrderId) {
        List<ProgressEvent> events = progressEventService.getTimeline(pickupOrderId);
        return R.ok(events);
    }

    @Operation(summary = "获取合同时间线")
    @GetMapping("/contract/{contractId}")
    public R<List<ProgressEvent>> getContractTimeline(@PathVariable Long contractId) {
        List<ProgressEvent> events = progressEventService.getContractTimeline(contractId);
        return R.ok(events);
    }
}
