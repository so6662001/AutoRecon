package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.common.exception.ErrorCode;
import com.pickupexpress.domain.dto.DeliveryCompleteDTO;
import com.pickupexpress.domain.dto.LiftUploadDTO;
import com.pickupexpress.common.util.TenantUtil;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.ContractItem;
import com.pickupexpress.domain.entity.DeliveryConfirm;
import com.pickupexpress.domain.entity.DeliveryPhoto;
import com.pickupexpress.domain.entity.LiftRecord;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.enums.DeliveryStatusEnum;
import com.pickupexpress.domain.enums.PickupCodeStatusEnum;
import com.pickupexpress.domain.enums.PickupOrderStatusEnum;
import com.pickupexpress.domain.vo.DeliveryProgressVO;
import com.pickupexpress.mapper.ContractItemMapper;
import com.pickupexpress.mapper.ContractMapper;
import com.pickupexpress.mapper.DeliveryConfirmMapper;
import com.pickupexpress.mapper.DeliveryPhotoMapper;
import com.pickupexpress.mapper.LiftRecordMapper;
import com.pickupexpress.mapper.PickupOrderMapper;
import com.pickupexpress.service.ContractService;
import com.pickupexpress.service.DeliveryService;
import com.pickupexpress.service.ProgressEventService;
import com.pickupexpress.service.SettlementService;
import com.pickupexpress.service.TradingHabitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 发货服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final PickupOrderMapper pickupOrderMapper;
    private final ContractMapper contractMapper;
    private final LiftRecordMapper liftRecordMapper;
    private final ContractItemMapper contractItemMapper;
    private final DeliveryConfirmMapper deliveryConfirmMapper;
    private final DeliveryPhotoMapper deliveryPhotoMapper;
    private final SettlementService settlementService;
    private final ProgressEventService progressEventService;
    private final ContractService contractService;
    private final TradingHabitService tradingHabitService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean verifyPickupCode(String pickupCode, String vehiclePlate) {
        PickupOrder order = pickupOrderMapper.selectOne(
                new LambdaQueryWrapper<PickupOrder>()
                        .eq(PickupOrder::getPickupCode, pickupCode));
        if (order == null) {
            log.warn("Pickup code verification failed: invalid code");
            throw new BizException(ErrorCode.PICKUP_CODE_INVALID);
        }
        Contract contract = contractMapper.selectById(order.getContractId());
        if (contract != null) {
            TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        }
        if (order.getPickupCodeExpireAt() != null && order.getPickupCodeExpireAt().isBefore(LocalDateTime.now())) {
            log.warn("Pickup code verification failed: expired code, pickupOrderId={}", order.getId());
            throw new BizException(ErrorCode.PICKUP_CODE_EXPIRED);
        }
        if (vehiclePlate != null && !vehiclePlate.isBlank() && order.getVehiclePlate() != null
                && !vehiclePlate.trim().equalsIgnoreCase(order.getVehiclePlate().trim())) {
            log.warn("Pickup code verification failed: vehicle plate mismatch, pickupOrderId={}", order.getId());
            return false;
        }
        order.setPickupCodeStatus(PickupCodeStatusEnum.VERIFIED.getValue());
        order.setDeliveryStatus(1); // 发货中
        order.setStatus(PickupOrderStatusEnum.DELIVERING.getValue());
        pickupOrderMapper.updateById(order);

        // 记录验证通过事件
        progressEventService.recordEvent(order.getId(), order.getContractId(),
                "PICKUP_CODE_VERIFIED",
                "提货码验证通过(车牌:" + (vehiclePlate != null ? vehiclePlate : "未填") + ")",
                null, "warehouse");

        log.info("Pickup code verified: pickupOrderId={}, code={}", order.getId(), pickupCode);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadLift(LiftUploadDTO dto) {
        PickupOrder order = pickupOrderMapper.selectById(dto.getPickupOrderId());
        if (order == null) {
            throw new BizException(ErrorCode.PICKUP_ORDER_NOT_FOUND);
        }
        if (order.getDeliveryStatus() != null && order.getDeliveryStatus() == 2) {
            throw new BizException(ErrorCode.DELIVERY_ALREADY_COMPLETED);
        }
        Contract contract = contractMapper.selectById(order.getContractId());
        if (contract != null) {
            TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        }

        Integer nextSeq = liftRecordMapper.selectCount(
                new LambdaQueryWrapper<LiftRecord>().eq(LiftRecord::getPickupOrderId, dto.getPickupOrderId())).intValue() + 1;

        LiftRecord record = LiftRecord.builder()
                .pickupOrderId(dto.getPickupOrderId())
                .liftSeq(dto.getLiftSeq() != null ? dto.getLiftSeq() : nextSeq)
                .productName(dto.getProductName())
                .spec(dto.getSpec())
                .material(dto.getMaterial())
                .heatNo(dto.getHeatNo())
                .batchNo(dto.getBatchNo())
                .pieces(dto.getPieces())
                .theoreticalWeight(dto.getTheoreticalWeight())
                .actualWeight(dto.getActualWeight())
                .operatorId(dto.getOperatorId())
                .operatorName(dto.getOperatorName())
                .dataSource(dto.getDataSource())
                .uploadedAt(LocalDateTime.now())
                .build();
        liftRecordMapper.insert(record);

        List<LiftRecord> lifts = liftRecordMapper.selectList(
                new LambdaQueryWrapper<LiftRecord>().eq(LiftRecord::getPickupOrderId, dto.getPickupOrderId()));
        int totalLifts = lifts.size();
        int totalPieces = lifts.stream().mapToInt(l -> l.getPieces() != null ? l.getPieces() : 0).sum();
        BigDecimal totalWeight = lifts.stream()
                .map(l -> l.getActualWeight() != null ? l.getActualWeight() : l.getTheoreticalWeight() != null ? l.getTheoreticalWeight() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalLifts(totalLifts);
        order.setTotalPieces(totalPieces);
        order.setTotalWeight(totalWeight);
        order.setDeliveryStatus(DeliveryStatusEnum.IN_PROGRESS.getValue());
        pickupOrderMapper.updateById(order);
    }

    @Override
    public DeliveryProgressVO getDeliveryProgress(Long pickupOrderId) {
        PickupOrder order = pickupOrderMapper.selectById(pickupOrderId);
        if (order == null) {
            throw new BizException(ErrorCode.PICKUP_ORDER_NOT_FOUND);
        }

        List<LiftRecord> lifts = liftRecordMapper.selectList(
                new LambdaQueryWrapper<LiftRecord>().eq(LiftRecord::getPickupOrderId, pickupOrderId));

        DeliveryProgressVO vo = new DeliveryProgressVO();
        vo.setPickupOrderId(pickupOrderId);
        vo.setTotalLifts(order.getTotalLifts() != null ? order.getTotalLifts() : 0);
        vo.setCompletedLifts(lifts.size());
        vo.setTotalWeight(order.getTotalWeight());
        vo.setCurrentWeight(lifts.stream()
                .map(l -> l.getActualWeight() != null ? l.getActualWeight() : l.getTheoreticalWeight() != null ? l.getTheoreticalWeight() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        vo.setLifts(lifts);
        vo.setStatus(order.getDeliveryStatus());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeDelivery(DeliveryCompleteDTO dto) {
        PickupOrder order = pickupOrderMapper.selectById(dto.getPickupOrderId());
        if (order == null) {
            throw new BizException(ErrorCode.PICKUP_ORDER_NOT_FOUND);
        }
        if (order.getDeliveryStatus() != null && order.getDeliveryStatus() == 2) {
            return;
        }
        Contract contract = contractMapper.selectById(order.getContractId());
        if (contract != null) {
            TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        }

        DeliveryConfirm confirm = DeliveryConfirm.builder()
                .pickupOrderId(dto.getPickupOrderId())
                .operatorId(dto.getOperatorId())
                .operatorName(dto.getOperatorName())
                .signatureUrl(dto.getSignatureUrl())
                .totalLifts(order.getTotalLifts())
                .totalPieces(order.getTotalPieces())
                .totalWeight(order.getTotalWeight())
                .confirmedAt(LocalDateTime.now())
                .remark(dto.getRemark())
                .build();
        deliveryConfirmMapper.insert(confirm);

        order.setDeliveryStatus(DeliveryStatusEnum.COMPLETED.getValue());
        order.setStatus(PickupOrderStatusEnum.COMPLETED.getValue());
        pickupOrderMapper.updateById(order);

        settlementService.generateSettlement(dto.getPickupOrderId());

        List<LiftRecord> lifts = liftRecordMapper.selectList(
                new LambdaQueryWrapper<LiftRecord>().eq(LiftRecord::getPickupOrderId, order.getId()));
        BigDecimal actualWeight = lifts.stream()
                .map(l -> l.getActualWeight() != null ? l.getActualWeight() : l.getTheoreticalWeight())
                .filter(w -> w != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try {
            tradingHabitService.recordPickup(order.getBuyerId(),
                    order.getDriverName(), order.getDriverPhone(), order.getVehiclePlate(),
                    actualWeight, order.getTotalAmount());
        } catch (Exception e) {
            log.warn("Failed to record trading habit: {}", e.getMessage());
        }

        generatePickupConfirmation(order);
    }

    private void generatePickupConfirmation(PickupOrder order) {
        if (order.getContractId() == null) {
            return;
        }

        List<ContractItem> contractItems = contractItemMapper.selectList(
                new LambdaQueryWrapper<ContractItem>().eq(ContractItem::getContractId, order.getContractId()));

        List<LiftRecord> lifts = liftRecordMapper.selectList(
                new LambdaQueryWrapper<LiftRecord>().eq(LiftRecord::getPickupOrderId, order.getId()));

        BigDecimal actualWeight = lifts.stream()
                .map(l -> l.getActualWeight() != null ? l.getActualWeight() : l.getTheoreticalWeight())
                .filter(w -> w != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal contractWeight = contractItems.stream()
                .map(i -> i.getWeight() != null ? i.getWeight() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (contractWeight.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diffRate = actualWeight.subtract(contractWeight).abs()
                    .divide(contractWeight, 4, RoundingMode.HALF_UP);

            String varianceType;
            if (diffRate.compareTo(BigDecimal.valueOf(0.03)) > 0) {
                varianceType = "超差";
                log.info("Pickup confirmation: OVER_TOLERANCE variance {}% for order {}",
                        diffRate.multiply(BigDecimal.valueOf(100)), order.getPickupNo());
            } else if (diffRate.compareTo(BigDecimal.ZERO) > 0) {
                varianceType = "容差内";
            } else {
                varianceType = "无差异";
            }

            progressEventService.recordEvent(order.getId(), order.getContractId(),
                    "PICKUP_CONFIRMATION_GENERATED",
                    "提货确认单生成(" + varianceType + "): 合同" + contractWeight + "吨, 实际" + actualWeight + "吨",
                    null, "system");
        }

        contractService.updatePickedAmount(order.getContractId(), actualWeight, order.getTotalAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadPhoto(Long pickupOrderId, DeliveryPhoto photo) {
        PickupOrder order = pickupOrderMapper.selectById(pickupOrderId);
        if (order == null) {
            throw new BizException(ErrorCode.PICKUP_ORDER_NOT_FOUND);
        }
        photo.setPickupOrderId(pickupOrderId);
        deliveryPhotoMapper.insert(photo);
    }
}
