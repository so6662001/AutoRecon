package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.domain.dto.AnalyticsCollectDTO;
import com.autorecon.domain.vo.FunnelDataVO;
import com.autorecon.domain.vo.PageValueVO;
import com.autorecon.domain.vo.RealtimeOverviewVO;
import com.autorecon.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 数据分析 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "数据分析")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/collect")
    @Operation(summary = "采集事件数据")
    public R<Void> collectEvents(@Valid @RequestBody AnalyticsCollectDTO dto,
                                 HttpServletRequest request) {
        String ip = getClientIp(request);
        analyticsService.collectEvents(dto, ip);
        return R.ok();
    }

    @GetMapping("/realtime/overview")
    @Operation(summary = "实时概览")
    public R<RealtimeOverviewVO> getRealtimeOverview(
            @RequestParam(value = "system", defaultValue = "autorecon") String system) {
        return R.ok(analyticsService.getRealtimeOverview(system));
    }

    @GetMapping("/pages/ranking")
    @Operation(summary = "页面价值排行")
    public R<List<PageValueVO>> getPageRanking(
            @RequestParam(value = "system", defaultValue = "autorecon") String system,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "module", required = false) String module) {
        return R.ok(analyticsService.getPageRanking(system, startDate, endDate, module));
    }

    @GetMapping("/realtime/hot-pages")
    @Operation(summary = "热门页面")
    public R<List<Map<String, Object>>> getHotPages(
            @RequestParam(value = "system", defaultValue = "autorecon") String system,
            @RequestParam(value = "topN", defaultValue = "10") int topN) {
        return R.ok(analyticsService.getHotPages(system, topN));
    }

    @GetMapping("/actions/stats")
    @Operation(summary = "操作统计")
    public R<List<Map<String, Object>>> getActionStats(
            @RequestParam(value = "system", defaultValue = "autorecon") String system,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(analyticsService.getActionStats(system, startDate, endDate));
    }

    @GetMapping("/funnel/{id}/data")
    @Operation(summary = "漏斗数据")
    public R<FunnelDataVO> getFunnelData(
            @PathVariable("id") Long id,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        FunnelDataVO data = analyticsService.getFunnelData(id, startDate, endDate);
        return data != null ? R.ok(data) : R.fail("漏斗配置不存在");
    }

    @GetMapping("/retention")
    @Operation(summary = "留存分析")
    public R<Map<String, Object>> getRetentionData(
            @RequestParam(value = "system", defaultValue = "autorecon") String system,
            @RequestParam(value = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "days", defaultValue = "7") int days) {
        return R.ok(analyticsService.getRetentionData(system, startDate, days));
    }

    @GetMapping("/device/distribution")
    @Operation(summary = "设备分布")
    public R<Map<String, Object>> getDeviceDistribution(
            @RequestParam(value = "system", defaultValue = "autorecon") String system,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(analyticsService.getDeviceDistribution(system, startDate, endDate));
    }

    @GetMapping("/performance/overview")
    @Operation(summary = "性能概览")
    public R<Map<String, Object>> getPerformanceOverview(
            @RequestParam(value = "system", defaultValue = "autorecon") String system,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(analyticsService.getPerformanceOverview(system, startDate, endDate));
    }

    @GetMapping("/features/adoption")
    @Operation(summary = "功能采用率")
    public R<List<Map<String, Object>>> getFeatureAdoption(
            @RequestParam(value = "system", defaultValue = "autorecon") String system,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(analyticsService.getFeatureAdoption(system, startDate, endDate));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
