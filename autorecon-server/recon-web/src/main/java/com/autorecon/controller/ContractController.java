package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.vo.ContractDiffVO;
import com.autorecon.domain.vo.ContractSummaryVO;
import com.autorecon.service.ContractReconService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 合同对账 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/contracts")
@Tag(name = "合同对账")
@RequiredArgsConstructor
public class ContractController {

    private final ContractReconService contractReconService;

    @GetMapping("/")
    @Operation(summary = "列出合同")
    public R<List<ContractSummaryVO>> listContracts(
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Long buyerId,
            @RequestParam(required = false) LocalDate periodStart,
            @RequestParam(required = false) LocalDate periodEnd) {
        List<ContractSummaryVO> list = contractReconService.listContracts(sellerId, buyerId, periodStart, periodEnd);
        return R.ok(list);
    }

    @GetMapping("/{contractNo}/items")
    @Operation(summary = "获取合同明细")
    public R<List<ReconBillItem>> getContractItems(@PathVariable String contractNo) {
        List<ReconBillItem> items = contractReconService.getContractItems(contractNo);
        return R.ok(items);
    }

    @GetMapping("/{contractNo}/summary")
    @Operation(summary = "获取合同汇总")
    public R<ContractSummaryVO> getContractSummary(@PathVariable String contractNo) {
        ContractSummaryVO summary = contractReconService.getContractSummary(contractNo);
        return R.ok(summary);
    }

    @GetMapping("/bill/{billId}/group")
    @Operation(summary = "按合同分组对账单明细")
    public R<Map<String, List<ReconBillItem>>> getBillGroupByContract(@PathVariable Long billId) {
        Map<String, List<ReconBillItem>> group = contractReconService.getBillGroupByContract(billId);
        return R.ok(group);
    }

    @GetMapping("/bill/{billId}/diff")
    @Operation(summary = "获取对账单各合同差异统计")
    public R<Map<String, ContractDiffVO>> getBillContractDiff(@PathVariable Long billId) {
        Map<String, ContractDiffVO> diff = contractReconService.getBillContractDiff(billId);
        return R.ok(diff);
    }
}
