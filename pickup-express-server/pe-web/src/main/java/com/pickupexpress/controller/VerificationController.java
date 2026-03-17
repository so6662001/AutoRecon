package com.pickupexpress.controller;

import com.pickupexpress.common.result.R;
import com.pickupexpress.domain.vo.VerificationResultVO;
import com.pickupexpress.service.PickupVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "提货确权")
@RestController
@RequestMapping("/api/v1/evidence/verification")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final PickupVerificationService pickupVerificationService;

    @Operation(summary = "确权验证")
    @PostMapping("/verify")
    public R<VerificationResultVO> verify(
            @RequestParam Long pickupOrderId,
            @RequestParam String driverName,
            @RequestParam String driverPhone,
            @RequestParam String vehiclePlate) {
        VerificationResultVO vo = pickupVerificationService.verify(pickupOrderId, driverName, driverPhone, vehiclePlate);
        return R.ok(vo);
    }

    @Operation(summary = "记录通话结果")
    @PostMapping("/{id}/phone-result")
    public R<Void> recordPhoneCallResult(
            @PathVariable Long id,
            @RequestParam Integer result,
            @RequestParam(required = false) String recordingUrl) {
        pickupVerificationService.recordPhoneCallResult(id, result, recordingUrl);
        return R.ok();
    }

    @Operation(summary = "上传驾驶员身份信息")
    @PostMapping("/{id}/identity")
    public R<Void> uploadDriverIdentity(
            @PathVariable Long id,
            @RequestParam(required = false) String idPhotoUrl,
            @RequestParam(required = false) String facePhotoUrl,
            @RequestParam(required = false) String licensePhotoUrl) {
        pickupVerificationService.uploadDriverIdentity(id, idPhotoUrl, facePhotoUrl, licensePhotoUrl);
        return R.ok();
    }
}
