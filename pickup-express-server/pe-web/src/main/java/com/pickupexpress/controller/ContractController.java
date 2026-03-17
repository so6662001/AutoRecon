package com.pickupexpress.controller;

import com.pickupexpress.common.result.PageResult;
import com.pickupexpress.common.result.R;
import com.pickupexpress.domain.dto.ContractQueryDTO;
import com.pickupexpress.domain.entity.ProgressEvent;
import com.pickupexpress.domain.vo.ContractDetailVO;
import com.pickupexpress.domain.vo.ContractVO;
import com.pickupexpress.service.ContractService;
import com.pickupexpress.service.ProgressEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "合同管理")
@RestController
@RequestMapping("/api/v1/evidence/contracts")
@RequiredArgsConstructor
@Slf4j
public class ContractController {

    private final ContractService contractService;
    private final ProgressEventService progressEventService;

    @Operation(summary = "分页查询合同")
    @GetMapping("/")
    public R<PageResult<ContractVO>> queryContracts(ContractQueryDTO query) {
        PageResult<ContractVO> result = contractService.queryContracts(query);
        return R.ok(result);
    }

    @Operation(summary = "获取合同详情")
    @GetMapping("/{id}")
    public R<ContractDetailVO> getContractDetail(@PathVariable Long id) {
        ContractDetailVO vo = contractService.getContractDetail(id);
        return R.ok(vo);
    }

    @Operation(summary = "发起签约")
    @PostMapping("/{id}/sign-request")
    public R<Void> initiateSign(@PathVariable Long id) {
        contractService.initiateSign(id);
        return R.ok();
    }

    @Operation(summary = "客户签约")
    @PostMapping("/{id}/sign")
    public R<Void> customerSign(@PathVariable Long id) {
        contractService.customerSign(id);
        return R.ok();
    }

    @Operation(summary = "获取合同进度时间线")
    @GetMapping("/{id}/progress")
    public R<List<ProgressEvent>> getContractTimeline(@PathVariable Long id) {
        List<ProgressEvent> events = progressEventService.getContractTimeline(id);
        return R.ok(events);
    }
}
