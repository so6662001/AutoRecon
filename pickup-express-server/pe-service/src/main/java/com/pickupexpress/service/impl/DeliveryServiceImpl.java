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
import com.pickupexpress.service.EvidenceService;
import com.pickupexpress.service.NotificationService;
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
    private final NotificationService notificationService;
    private final org.springframework.context.ApplicationContext applicationContext;

    private EvidenceService getEvidenceService() {
        return applicationContext.getBean(EvidenceService.class);
    }

    // 提货码验证失败计数器: key=pickupCode, value=失败次数
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger>
            VERIFY_FAIL_COUNTER = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean verifyPickupCode(String pickupCode, String vehiclePlate) {
        // 安全: 5次错误锁定检查(设计十二: 暴力猜测防护)
        java.util.concurrent.atomic.AtomicInteger failCount =
                VERIFY_FAIL_COUNTER.computeIfAbsent(pickupCode, k -> new java.util.concurrent.atomic.AtomicInteger(0));
        if (failCount.get() >= MAX_VERIFY_ATTEMPTS) {
            log.warn("Pickup code locked after {} failed attempts: code={}", MAX_VERIFY_ATTEMPTS, pickupCode);
            throw new BizException(ErrorCode.PICKUP_CODE_INVALID.getCode(), "提货码已被锁定(连续" + MAX_VERIFY_ATTEMPTS + "次验证失败)，请联系销售重新生成");
        }

        PickupOrder order = pickupOrderMapper.selectOne(
                new LambdaQueryWrapper<PickupOrder>()
                        .eq(PickupOrder::getPickupCode, pickupCode));
        if (order == null) {
            failCount.incrementAndGet();
            log.warn("Pickup code verification failed: invalid code, failCount={}", failCount.get());
            throw new BizException(ErrorCode.PICKUP_CODE_INVALID);
        }

        // 安全: 一次性使用检查(设计十二: 一次性使用)
        if (order.getPickupCodeStatus() != null
                && order.getPickupCodeStatus() >= PickupCodeStatusEnum.VERIFIED.getValue()) {
            log.warn("Pickup code already used/verified: pickupOrderId={}, status={}",
                    order.getId(), order.getPickupCodeStatus());
            // 已验证的允许继续(同一次提货)，已使用的拒绝
            if (order.getPickupCodeStatus() == PickupCodeStatusEnum.USED.getValue()) {
                throw new BizException(ErrorCode.PICKUP_CODE_INVALID.getCode(), "提货码已使用，不可重复提货");
            }
            if (order.getPickupCodeStatus() == PickupCodeStatusEnum.EXPIRED.getValue()) {
                throw new BizException(ErrorCode.PICKUP_CODE_EXPIRED);
            }
        }

        Contract contract = contractMapper.selectById(order.getContractId());
        if (contract != null) {
            TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        }

        // 过期检查
        if (order.getPickupCodeExpireAt() != null && order.getPickupCodeExpireAt().isBefore(LocalDateTime.now())) {
            failCount.incrementAndGet();
            order.setPickupCodeStatus(PickupCodeStatusEnum.EXPIRED.getValue());
            pickupOrderMapper.updateById(order);
            log.warn("Pickup code verification failed: expired code, pickupOrderId={}", order.getId());
            throw new BizException(ErrorCode.PICKUP_CODE_EXPIRED);
        }

        // 车牌比对
        if (vehiclePlate != null && !vehiclePlate.isBlank() && order.getVehiclePlate() != null
                && !vehiclePlate.trim().equalsIgnoreCase(order.getVehiclePlate().trim())) {
            failCount.incrementAndGet();
            log.warn("Pickup code verification failed: vehicle plate mismatch, pickupOrderId={}, failCount={}",
                    order.getId(), failCount.get());
            return false;
        }

        // 验证通过 — 重置失败计数
        VERIFY_FAIL_COUNTER.remove(pickupCode);

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

        // 进度事件: 逐吊上传(每5吊记录一次,避免事件过多)
        if (totalLifts == 1 || totalLifts % 5 == 0) {
            String dataSourceName = dto.getDataSource() != null ? switch (dto.getDataSource()) {
                case 1 -> "WMS";
                case 2 -> "H5助手";
                case 3 -> "第三方WMS";
                case 4 -> "驾驶员";
                case 5 -> "补录";
                default -> "未知";
            } : "未知";
            progressEventService.recordEvent(order.getId(), order.getContractId(),
                    "LIFT_UPLOADED",
                    "已装第" + totalLifts + "吊, 累计" + totalWeight.toPlainString() + "吨 (来源:" + dataSourceName + ")",
                    null, dto.getOperatorName());
        }
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
        order.setPickupCodeStatus(PickupCodeStatusEnum.USED.getValue()); // 提货码标记已使用
        pickupOrderMapper.updateById(order);

        // 进度事件: 发货完成
        progressEventService.recordEvent(order.getId(), order.getContractId(),
                "DELIVERY_COMPLETED",
                "发货完成: 共" + order.getTotalLifts() + "吊/" + order.getTotalPieces() + "件/" + order.getTotalWeight() + "吨",
                null, dto.getOperatorName());

        // 进度事件: 仓库签字确认
        progressEventService.recordEvent(order.getId(), order.getContractId(),
                "DELIVERY_SIGNED",
                "仓库操作员" + (dto.getOperatorName() != null ? dto.getOperatorName() : "") + "签字确认",
                null, dto.getOperatorName());

        settlementService.generateSettlement(dto.getPickupOrderId());

        // 通知买方提货完成(15.2原则: 事后通知, 用语友好)
        try {
            notificationService.sendPickupCompleteNotification(dto.getPickupOrderId(),
                    order.getTotalWeight(), order.getTotalAmount());
        } catch (Exception e) {
            log.warn("Failed to send pickup complete notification: {}", e.getMessage());
        }

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

        // 自动归档证据包(设计4.8: 发货完成后自动归档)
        try {
            getEvidenceService().archiveEvidence(order.getId());
        } catch (Exception e) {
            log.warn("Failed to auto-archive evidence: {}", e.getMessage());
        }
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
