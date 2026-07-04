package com.pickupexpress.controller;

import com.pickupexpress.common.result.PageResult;
import com.pickupexpress.common.result.R;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.vo.PickupOrderDetailVO;
import com.pickupexpress.domain.vo.PickupOrderVO;
import com.pickupexpress.service.PickupOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "提货单管理")
@RestController
@RequestMapping("/api/v1/evidence/pickup")
@RequiredArgsConstructor
@Slf4j
public class PickupOrderController {

    private final PickupOrderService pickupOrderService;

    @Operation(summary = "分页查询提货单")
    @GetMapping("/")
    public R<PageResult<PickupOrderVO>> queryPickupOrders(
            @RequestParam(required = false) Long contractId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<PickupOrderVO> result = pickupOrderService.queryPickupOrders(contractId, status, pageNum, pageSize);
        return R.ok(result);
    }

    @Operation(summary = "获取提货单详情")
    @GetMapping("/{id}")
    public R<PickupOrderDetailVO> getPickupOrderDetail(@PathVariable Long id) {
        PickupOrderDetailVO vo = pickupOrderService.getPickupOrderDetail(id);
        return R.ok(vo);
    }

    @Operation(summary = "取消提货单")
    @PutMapping("/{id}/cancel")
    public R<Void> cancelPickupOrder(@PathVariable Long id) {
        pickupOrderService.cancelPickupOrder(id);
        return R.ok();
    }

    @Operation(summary = "获取提货码")
    @GetMapping("/{id}/qrcode")
    public R<Map<String, String>> getPickupCode(@PathVariable Long id) {
        PickupOrder order = pickupOrderService.getById(id);
        if (order == null) {
            return R.fail("提货单不存在");
        }
        Map<String, String> result = new HashMap<>();
        result.put("code", order.getPickupCode());
        result.put("qrUrl", order.getPickupCodeQr() != null ? order.getPickupCodeQr() : "");
        return R.ok(result);
    }
}
