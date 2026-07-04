package com.pickupexpress.controller;

import com.pickupexpress.common.result.R;
import com.pickupexpress.domain.entity.TradingHabitRecord;
import com.pickupexpress.service.TradingHabitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "交易习惯")
@RestController
@RequestMapping("/api/v1/evidence/trading-habits")
@RequiredArgsConstructor
@Slf4j
public class TradingHabitController {

    private final TradingHabitService tradingHabitService;

    @Operation(summary = "按买方查询交易习惯")
    @GetMapping("/")
    public R<List<TradingHabitRecord>> listByBuyer(@RequestParam Long buyerId) {
        List<TradingHabitRecord> list = tradingHabitService.listByBuyer(buyerId);
        return R.ok(list);
    }

    @Operation(summary = "生成交易习惯报告")
    @GetMapping("/{buyerId}/report")
    public R<String> generateReport(@PathVariable Long buyerId) {
        String report = tradingHabitService.generateHabitReport(buyerId);
        return R.ok(report);
    }
}
