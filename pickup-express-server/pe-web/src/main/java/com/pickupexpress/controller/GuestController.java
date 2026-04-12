package com.pickupexpress.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.common.exception.ErrorCode;
import com.pickupexpress.common.result.R;
import com.pickupexpress.domain.dto.DispatchConfirmDTO;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.DeliveryPhoto;
import com.pickupexpress.domain.entity.LiftRecord;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.entity.SettlementOrder;
import com.pickupexpress.domain.enums.PickupOrderStatusEnum;
import com.pickupexpress.domain.vo.GuestPickupVO;
import com.pickupexpress.mapper.ContractMapper;
import com.pickupexpress.mapper.DeliveryPhotoMapper;
import com.pickupexpress.mapper.LiftRecordMapper;
import com.pickupexpress.mapper.SettlementOrderMapper;
import com.pickupexpress.service.PickupOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 买方免注册访问（演示：token 即提货单 ID）
 */
@RestController
@RequestMapping("/api/v1/guest")
@Tag(name = "买方免注册访问")
@RequiredArgsConstructor
@Slf4j
public class GuestController {

    private final PickupOrderService pickupOrderService;
    private final LiftRecordMapper liftRecordMapper;
    private final DeliveryPhotoMapper deliveryPhotoMapper;
    private final SettlementOrderMapper settlementOrderMapper;
    private final ContractMapper contractMapper;

    @Operation(summary = "免注册查看提货详情")
    @GetMapping("/pickup/{token}")
    public R<GuestPickupVO> getGuestPickupDetail(@PathVariable String token) {
        long pickupOrderId;
        try {
            pickupOrderId = Long.parseLong(token);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        PickupOrder order = pickupOrderService.getById(pickupOrderId);
        if (order == null) {
            throw new BizException(ErrorCode.PICKUP_ORDER_NOT_FOUND);
        }
        Contract contract = order.getContractId() != null ? contractMapper.selectById(order.getContractId()) : null;

        GuestPickupVO vo = new GuestPickupVO();
        vo.setPickupNo(order.getPickupNo());
        vo.setContractNo(order.getContractNo());
        vo.setBuyerName(parseErpBuyerName(contract));
        vo.setSellerName(null);
        vo.setTotalWeight(order.getTotalWeight());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setWarehouseName(order.getWarehouseName());
        vo.setDriverName(order.getDriverName());
        vo.setVehiclePlate(order.getVehiclePlate());
        if (order.getStatus() != null && order.getStatus() == PickupOrderStatusEnum.COMPLETED.getValue()) {
            vo.setCompletedAt(order.getUpdatedAt());
        }

        List<LiftRecord> lifts = liftRecordMapper.selectList(
                new LambdaQueryWrapper<LiftRecord>().eq(LiftRecord::getPickupOrderId, pickupOrderId));
        List<GuestPickupVO.GuestPickupItemVO> items = lifts.stream().map(l -> {
            GuestPickupVO.GuestPickupItemVO row = new GuestPickupVO.GuestPickupItemVO();
            row.setProductName(l.getProductName());
            row.setSpec(l.getSpec());
            row.setPieces(l.getPieces());
            row.setWeight(l.getActualWeight() != null ? l.getActualWeight() : l.getTheoreticalWeight());
            return row;
        }).collect(Collectors.toList());
        vo.setItems(items);

        List<DeliveryPhoto> photos = deliveryPhotoMapper.selectList(
                new LambdaQueryWrapper<DeliveryPhoto>().eq(DeliveryPhoto::getPickupOrderId, pickupOrderId));
        vo.setPhotos(photos.stream().map(DeliveryPhoto::getPhotoUrl).collect(Collectors.toList()));

        SettlementOrder settlement = settlementOrderMapper.selectOne(
                new LambdaQueryWrapper<SettlementOrder>().eq(SettlementOrder::getPickupOrderId, pickupOrderId));
        if (settlement != null) {
            vo.setSettlementAmount(settlement.getReceivableAmount() != null
                    ? settlement.getReceivableAmount()
                    : settlement.getTotalWithTax());
        }

        return R.ok(vo);
    }

    @Operation(summary = "免注册确认")
    @PostMapping("/pickup/{token}/confirm")
    public R<Void> confirmGuestPickup(@PathVariable String token, @RequestParam(required = false) String phone) {
        long pickupOrderId;
        try {
            pickupOrderId = Long.parseLong(token);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (phone != null) {
            log.info("Guest confirm pickup {} with phone {}", pickupOrderId, phone);
        }
        DispatchConfirmDTO dto = new DispatchConfirmDTO();
        dto.setPickupOrderId(pickupOrderId);
        dto.setConfirmed(true);
        pickupOrderService.confirmDispatch(dto);
        return R.ok();
    }

    private static String parseErpBuyerName(Contract contract) {
        if (contract == null || contract.getCustomClauses() == null) {
            return null;
        }
        String prefix = "erpBuyerName=";
        String clauses = contract.getCustomClauses();
        if (clauses.startsWith(prefix)) {
            return clauses.substring(prefix.length());
        }
        return null;
    }
}
