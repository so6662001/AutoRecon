package com.autorecon.controller;

import com.autorecon.common.result.PageResult;
import com.autorecon.common.result.R;
import com.autorecon.domain.dto.ReconBillCreateDTO;
import com.autorecon.domain.dto.ReconBillQueryDTO;
import com.autorecon.domain.vo.ReconBillDetailVO;
import com.autorecon.domain.vo.ReconBillVO;
import com.autorecon.service.ReconBillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 对账单管理 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/bills")
@Tag(name = "对账单管理")
@RequiredArgsConstructor
public class ReconBillController {

    private final ReconBillService reconBillService;

    @PostMapping("/")
    @Operation(summary = "创建对账单")
    public R<Long> createBill(@Valid @RequestBody ReconBillCreateDTO dto) {
        Long id = reconBillService.createBill(dto);
        return R.ok(id);
    }

    @GetMapping("/")
    @Operation(summary = "分页查询对账单列表")
    public R<PageResult<ReconBillVO>> queryBillList(ReconBillQueryDTO query) {
        PageResult<ReconBillVO> result = reconBillService.queryBillList(query);
        return R.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取对账单详情")
    public R<ReconBillDetailVO> getBillDetail(@PathVariable Long id) {
        ReconBillDetailVO detail = reconBillService.getBillDetail(id);
        return R.ok(detail);
    }

    @PutMapping("/{id}/send")
    @Operation(summary = "发送对账单")
    public R<Void> sendBill(@PathVariable Long id) {
        reconBillService.sendBill(id);
        return R.ok();
    }

    @PutMapping("/{id}/confirm")
    @Operation(summary = "确认对账单")
    public R<Void> confirmBill(@PathVariable Long id) {
        reconBillService.confirmBill(id);
        return R.ok();
    }

    @PutMapping("/{id}/void")
    @Operation(summary = "作废对账单")
    public R<Void> voidBill(@PathVariable Long id) {
        reconBillService.voidBill(id);
        return R.ok();
    }

    @PostMapping("/{id}/pdf")
    @Operation(summary = "生成对账单PDF")
    public R<String> generatePdf(@PathVariable Long id) {
        String result = reconBillService.generatePdf(id);
        return R.ok(result);
    }

    @PostMapping("/batch")
    @Operation(summary = "批量创建对账单")
    public R<String> batchCreateBills(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> buyerIdsRaw = (List<Number>) body.get("buyerIds");
        if (buyerIdsRaw == null || buyerIdsRaw.isEmpty()) {
            return R.fail("buyerIds不能为空");
        }
        List<Long> buyerIds = buyerIdsRaw.stream()
                .map(Number::longValue)
                .collect(Collectors.toList());
        LocalDate periodStart = LocalDate.parse((String) body.get("periodStart"));
        LocalDate periodEnd = LocalDate.parse((String) body.get("periodEnd"));
        Long templateId = ((Number) body.get("templateId")).longValue();
        String batchId = reconBillService.batchCreateBills(buyerIds, periodStart, periodEnd, templateId);
        return R.ok(batchId);
    }
}
