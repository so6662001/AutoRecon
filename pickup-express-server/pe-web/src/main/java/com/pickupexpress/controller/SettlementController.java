package com.pickupexpress.controller;

import com.pickupexpress.common.result.R;
import com.pickupexpress.domain.dto.SettlementGenerateDTO;
import com.pickupexpress.domain.entity.SettlementOrder;
import com.pickupexpress.domain.vo.SettlementVO;
import com.pickupexpress.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "结算管理")
@RestController
@RequestMapping("/api/v1/evidence/settlement")
@RequiredArgsConstructor
@Slf4j
public class SettlementController {

    private final SettlementService settlementService;

    @Operation(summary = "生成结算单")
    @PostMapping("/generate")
    public R<Long> generateSettlement(@Valid @RequestBody SettlementGenerateDTO dto) {
        Long id = settlementService.generateSettlement(dto.getPickupOrderId());
        return R.ok(id);
    }

    @Operation(summary = "获取结算单")
    @GetMapping("/{id}")
    public R<SettlementVO> getSettlement(@PathVariable Long id) {
        SettlementVO vo = settlementService.getSettlement(id);
        return R.ok(vo);
    }

    @Operation(summary = "标记客户已查看")
    @PutMapping("/{id}/viewed")
    public R<Void> markCustomerViewed(@PathVariable Long id) {
        settlementService.markCustomerViewed(id);
        return R.ok();
    }

    @Operation(summary = "按合同查询结算单列表")
    @GetMapping("/contract/{contractId}")
    public R<List<SettlementOrder>> listByContract(@PathVariable Long contractId) {
        List<SettlementOrder> list = settlementService.listByContract(contractId);
        return R.ok(list);
    }
}
