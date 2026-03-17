package com.pickupexpress.controller;

import com.pickupexpress.common.result.R;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.vo.DeliveryProgressVO;
import com.pickupexpress.service.DeliveryService;
import com.pickupexpress.service.PickupOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "驾驶员端")
@RestController
@RequestMapping("/api/v1/evidence/driver")
@RequiredArgsConstructor
@Slf4j
public class DriverController {

    private final PickupOrderService pickupOrderService;
    private final DeliveryService deliveryService;

    @Operation(summary = "驾驶员订单列表")
    @GetMapping("/orders")
    public R<List<PickupOrder>> listOrders(@RequestParam String driverPhone) {
        List<PickupOrder> orders = pickupOrderService.listByDriverPhone(driverPhone);
        return R.ok(orders);
    }

    @Operation(summary = "驾驶员接单")
    @PutMapping("/orders/{id}/accept")
    public R<Void> driverAccept(@PathVariable Long id) {
        pickupOrderService.driverAccept(id);
        return R.ok();
    }

    @Operation(summary = "驾驶员到达")
    @PutMapping("/orders/{id}/arrive")
    public R<Void> driverArrive(
            @PathVariable Long id,
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng) {
        pickupOrderService.driverArrive(id, lat, lng);
        return R.ok();
    }

    @Operation(summary = "获取发货进度")
    @GetMapping("/orders/{id}/progress")
    public R<DeliveryProgressVO> getDeliveryProgress(@PathVariable Long id) {
        DeliveryProgressVO vo = deliveryService.getDeliveryProgress(id);
        return R.ok(vo);
    }

    @Operation(summary = "驾驶员签字(占位)")
    @PostMapping("/orders/{id}/sign")
    public R<Void> driverSign(@PathVariable Long id, @RequestParam String signatureUrl) {
        com.pickupexpress.domain.dto.DeliveryCompleteDTO dto = new com.pickupexpress.domain.dto.DeliveryCompleteDTO();
        dto.setPickupOrderId(id);
        dto.setSignatureUrl(signatureUrl);
        deliveryService.completeDelivery(dto);
        return R.ok();
    }
}
