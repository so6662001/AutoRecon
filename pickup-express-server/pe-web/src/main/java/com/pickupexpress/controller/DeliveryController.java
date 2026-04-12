package com.pickupexpress.controller;

import com.pickupexpress.common.result.R;
import com.pickupexpress.domain.dto.DeliveryCompleteDTO;
import com.pickupexpress.domain.dto.LiftUploadDTO;
import com.pickupexpress.domain.entity.DeliveryPhoto;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.vo.PickupOrderVO;
import com.pickupexpress.service.DeliveryService;
import com.pickupexpress.service.PickupOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Tag(name = "发货管理")
@RestController
@RequestMapping("/api/v1/evidence/delivery")
@RequiredArgsConstructor
@Slf4j
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final PickupOrderService pickupOrderService;

    @Operation(summary = "验证提货码")
    @PostMapping("/verify-code")
    public R<PickupOrderVO> verifyPickupCode(
            @RequestParam("pickupCode") String pickupCode,
            @RequestParam(required = false) String vehiclePlate) {
        boolean valid = deliveryService.verifyPickupCode(pickupCode, vehiclePlate);
        if (!valid) {
            return R.fail("验证失败");
        }
        PickupOrder order = pickupOrderService.getByPickupCode(pickupCode);
        if (order == null) {
            return R.fail("提货单不存在");
        }
        PickupOrderVO vo = new PickupOrderVO();
        BeanUtils.copyProperties(order, vo);
        return R.ok(vo);
    }

    @Operation(summary = "上传吊装记录")
    @PostMapping("/lift-upload")
    public R<Void> uploadLift(@Valid @RequestBody LiftUploadDTO dto) {
        deliveryService.uploadLift(dto);
        return R.ok();
    }

    @Operation(summary = "完成发货")
    @PostMapping("/complete")
    public R<Void> completeDelivery(@Valid @RequestBody DeliveryCompleteDTO dto) {
        deliveryService.completeDelivery(dto);
        return R.ok();
    }

    @Operation(summary = "提交签字")
    @PostMapping("/sign")
    public R<Void> sign(@Valid @RequestBody DeliveryCompleteDTO dto) {
        deliveryService.completeDelivery(dto);
        return R.ok();
    }

    @Operation(summary = "上传照片")
    @PostMapping("/photo-upload")
    public R<Void> uploadPhoto(
            @RequestParam Long pickupOrderId,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam Integer photoType,
            @RequestParam(required = false) BigDecimal gpsLat,
            @RequestParam(required = false) BigDecimal gpsLng) {
        String photoUrl = file != null && !file.isEmpty() ? "/uploads/" + file.getOriginalFilename() : null;
        DeliveryPhoto photo = DeliveryPhoto.builder()
                .pickupOrderId(pickupOrderId)
                .photoType(photoType)
                .photoUrl(photoUrl)
                .build();
        deliveryService.uploadPhoto(pickupOrderId, photo);
        return R.ok();
    }
}
