package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.common.exception.ErrorCode;
import com.pickupexpress.domain.dto.DeliveryCompleteDTO;
import com.pickupexpress.domain.dto.LiftUploadDTO;
import com.pickupexpress.common.util.TenantUtil;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.DeliveryConfirm;
import com.pickupexpress.domain.entity.DeliveryPhoto;
import com.pickupexpress.domain.entity.LiftRecord;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.enums.DeliveryStatusEnum;
import com.pickupexpress.domain.enums.PickupCodeStatusEnum;
import com.pickupexpress.domain.enums.PickupOrderStatusEnum;
import com.pickupexpress.domain.vo.DeliveryProgressVO;
import com.pickupexpress.mapper.ContractMapper;
import com.pickupexpress.mapper.DeliveryConfirmMapper;
import com.pickupexpress.mapper.DeliveryPhotoMapper;
import com.pickupexpress.mapper.LiftRecordMapper;
import com.pickupexpress.mapper.PickupOrderMapper;
import com.pickupexpress.service.DeliveryService;
import com.pickupexpress.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final DeliveryConfirmMapper deliveryConfirmMapper;
    private final DeliveryPhotoMapper deliveryPhotoMapper;
    private final SettlementService settlementService;

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
        pickupOrderMapper.updateById(order);
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
