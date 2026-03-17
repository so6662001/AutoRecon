package com.pickupexpress.controller;

import com.pickupexpress.common.result.R;
import com.pickupexpress.domain.vo.EvidencePackageVO;
import com.pickupexpress.service.EvidenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "证据归档")
@RestController
@RequestMapping("/api/v1/evidence/archive")
@RequiredArgsConstructor
@Slf4j
public class EvidenceController {

    private final EvidenceService evidenceService;

    @Operation(summary = "归档证据")
    @PostMapping("/{pickupOrderId}")
    public R<Long> archiveEvidence(@PathVariable Long pickupOrderId) {
        Long id = evidenceService.archiveEvidence(pickupOrderId);
        return R.ok(id);
    }

    @Operation(summary = "获取证据包")
    @GetMapping("/{pickupOrderId}")
    public R<EvidencePackageVO> getEvidencePackage(@PathVariable Long pickupOrderId) {
        EvidencePackageVO vo = evidenceService.getEvidencePackage(pickupOrderId);
        return R.ok(vo);
    }

    @Operation(summary = "验证证据完整性")
    @GetMapping("/{pickupOrderId}/verify")
    public R<Boolean> verifyIntegrity(@PathVariable Long pickupOrderId) {
        boolean valid = evidenceService.verifyIntegrityByPickupOrderId(pickupOrderId);
        return R.ok(valid);
    }
}
