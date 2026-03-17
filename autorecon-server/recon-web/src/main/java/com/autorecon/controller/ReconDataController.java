package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.domain.dto.ReconBillItemDTO;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.service.ReconDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 对账数据 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/data")
@Tag(name = "对账数据")
@RequiredArgsConstructor
public class ReconDataController {

    private final ReconDataService reconDataService;

    @PostMapping("/upload-excel")
    @Operation(summary = "上传 Excel 导入买方数据")
    public R<List<ReconBillItem>> uploadExcel(@RequestParam Long billId, @RequestParam("file") MultipartFile file) {
        List<ReconBillItem> items = reconDataService.uploadExcel(billId, file);
        return R.ok(items);
    }

    @PostMapping("/online-submit")
    @Operation(summary = "在线提交买方数据")
    public R<Void> onlineSubmit(@RequestBody Map<String, Object> body) {
        Long billId = ((Number) body.get("billId")).longValue();
        @SuppressWarnings("unchecked")
        List<ReconBillItemDTO> buyerItems = (List<ReconBillItemDTO>) body.get("buyerItems");
        reconDataService.onlineSubmit(billId, buyerItems != null ? buyerItems : List.of());
        return R.ok();
    }

    @GetMapping("/excel-mapping/{buyerId}")
    @Operation(summary = "获取 Excel 列映射配置")
    public R<Map<String, String>> getExcelMapping(@PathVariable Long buyerId) {
        Map<String, String> mapping = reconDataService.getExcelMapping(buyerId);
        return R.ok(mapping);
    }

    @PutMapping("/excel-mapping/{buyerId}")
    @Operation(summary = "保存 Excel 列映射配置")
    public R<Void> saveExcelMapping(@PathVariable Long buyerId, @RequestBody Map<String, String> mapping) {
        reconDataService.saveExcelMapping(buyerId, mapping);
        return R.ok();
    }
}
