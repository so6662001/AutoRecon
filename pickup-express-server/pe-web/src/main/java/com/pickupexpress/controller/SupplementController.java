package com.pickupexpress.controller;

import com.pickupexpress.common.result.R;
import com.pickupexpress.common.util.SecurityUtil;
import com.pickupexpress.domain.dto.SupplementCreateDTO;
import com.pickupexpress.domain.entity.SupplementRecord;
import com.pickupexpress.service.SupplementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "事后补录")
@RestController
@RequestMapping("/api/v1/evidence/supplement")
@RequiredArgsConstructor
@Slf4j
public class SupplementController {

    private final SupplementService supplementService;

    @Operation(summary = "创建补录")
    @PostMapping("/")
    public R<Long> createSupplement(@Valid @RequestBody SupplementCreateDTO dto) {
        Long id = supplementService.createSupplement(dto);
        return R.ok(id);
    }

    @Operation(summary = "审批通过")
    @PutMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id, @RequestParam(required = false) String comment) {
        supplementService.approve(id, comment);
        return R.ok();
    }

    @Operation(summary = "审批拒绝")
    @PutMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @RequestParam(required = false) String comment) {
        supplementService.reject(id, comment);
        return R.ok();
    }

    @Operation(summary = "待审批列表")
    @GetMapping("/pending")
    public R<List<SupplementRecord>> listPending() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            return R.fail("未获取到企业信息");
        }
        List<SupplementRecord> list = supplementService.listPending(enterpriseId);
        return R.ok(list);
    }
}
