package com.pickupexpress.controller;

import com.pickupexpress.common.result.R;
import com.pickupexpress.domain.dto.DispatchConfirmDTO;
import com.pickupexpress.domain.dto.DispatchRequestDTO;
import com.pickupexpress.domain.dto.DriverAssignDTO;
import com.pickupexpress.service.PickupOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "派车管理")
@RestController
@RequestMapping("/api/v1/evidence/dispatch")
@RequiredArgsConstructor
@Slf4j
public class DispatchController {

    private final PickupOrderService pickupOrderService;

    @Operation(summary = "创建派车单")
    @PostMapping("/request")
    public R<Long> createPickupOrder(@Valid @RequestBody DispatchRequestDTO dto) {
        Long id = pickupOrderService.createPickupOrder(dto);
        return R.ok(id);
    }

    @Operation(summary = "确认派车")
    @PutMapping("/{id}/confirm")
    public R<Void> confirmDispatch(@PathVariable Long id, @Valid @RequestBody DispatchConfirmDTO dto) {
        dto.setPickupOrderId(id);
        dto.setConfirmed(true);
        pickupOrderService.confirmDispatch(dto);
        return R.ok();
    }

    @Operation(summary = "拒绝派车")
    @PutMapping("/{id}/reject")
    public R<Void> rejectDispatch(@PathVariable Long id, @Valid @RequestBody DispatchConfirmDTO dto) {
        dto.setPickupOrderId(id);
        dto.setConfirmed(false);
        pickupOrderService.confirmDispatch(dto);
        return R.ok();
    }

    @Operation(summary = "分配驾驶员")
    @PutMapping("/{id}/assign-driver")
    public R<Void> assignDriver(@PathVariable Long id, @Valid @RequestBody DriverAssignDTO dto) {
        dto.setPickupOrderId(id);
        pickupOrderService.assignDriver(dto);
        return R.ok();
    }
}
