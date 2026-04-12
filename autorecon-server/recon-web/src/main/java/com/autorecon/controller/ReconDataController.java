package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.domain.dto.OnlineSubmitDTO;
import com.autorecon.domain.dto.ReconBillItemDTO;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.vo.ExcelAnalysisVO;
import com.autorecon.service.ReconDataService;
import com.autorecon.service.erp.ErpPullService;
import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
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
    private final ErpPullService erpPullService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "从ERP拉取对账数据")
    @PostMapping("/erp-pull")
    public R<List<Map<String, Object>>> pullFromErp(
            @RequestParam Long connectionId,
            @RequestParam String dataType,
            @RequestParam(required = false) String periodStart,
            @RequestParam(required = false) String periodEnd) {
        Map<String, String> params = new HashMap<>();
        if (periodStart != null) {
            params.put("periodStart", periodStart);
        }
        if (periodEnd != null) {
            params.put("periodEnd", periodEnd);
        }
        List<Map<String, Object>> data = erpPullService.pullData(connectionId, dataType, params);
        return R.ok(data);
    }

    @Operation(summary = "从ERP拉取并导入到对账单")
    @PostMapping("/erp-import")
    public R<Integer> importFromErp(
            @RequestParam Long connectionId,
            @RequestParam Long billId,
            @RequestParam(defaultValue = "order") String dataType) {
        int count = erpPullService.importFromErp(connectionId, billId, dataType);
        return R.ok(count);
    }

    @PostMapping("/upload-excel")
    @Operation(summary = "上传 Excel 导入买方数据")
    public R<List<ReconBillItem>> uploadExcel(@RequestParam Long billId, @RequestParam("file") MultipartFile file) {
        List<ReconBillItem> items = reconDataService.uploadExcel(billId, file);
        return R.ok(items);
    }

    @PostMapping("/online-submit")
    @Operation(summary = "在线提交买方数据")
    public R<Void> onlineSubmit(@Valid @RequestBody OnlineSubmitDTO dto) {
        reconDataService.onlineSubmit(dto.getBillId(), dto.getBuyerItems() != null ? dto.getBuyerItems() : List.of());
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

    @PostMapping("/analyze-headers")
    @Operation(summary = "分析 Excel 表头并返回映射建议")
    public R<ExcelAnalysisVO> analyzeExcelHeaders(@RequestParam("file") MultipartFile file, @RequestParam Long buyerId) {
        ExcelAnalysisVO vo = reconDataService.analyzeExcelHeaders(file, buyerId);
        return R.ok(vo);
    }

    @PostMapping("/upload-excel-with-mapping")
    @Operation(summary = "使用指定映射解析 Excel 导入买方数据")
    public R<List<ReconBillItem>> uploadExcelWithMapping(
            @RequestParam Long billId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String mappingJson,
            @RequestParam(required = false) Boolean saveMapping,
            @RequestParam(required = false) Long buyerId) {
        Map<String, String> mapping = null;
        if (mappingJson != null && !mappingJson.isBlank()) {
            try {
                mapping = objectMapper.readValue(mappingJson, new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("Invalid mappingJson", e);
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "mappingJson 格式不正确");
            }
        }
        List<ReconBillItem> items = reconDataService.uploadExcelWithMapping(billId, file, mapping, saveMapping, buyerId);
        return R.ok(items);
    }
}
