package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.common.exception.ErrorCode;
import com.pickupexpress.common.util.TenantUtil;
import com.pickupexpress.common.result.PageResult;
import com.pickupexpress.domain.dto.DispatchConfirmDTO;
import com.pickupexpress.domain.dto.DispatchRequestDTO;
import com.pickupexpress.domain.dto.DriverAssignDTO;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.enums.ContractStatusEnum;
import com.pickupexpress.domain.enums.ContractTypeEnum;
import com.pickupexpress.domain.enums.DispatchModeEnum;
import com.pickupexpress.domain.enums.PickupCodeStatusEnum;
import com.pickupexpress.domain.enums.PickupOrderStatusEnum;
import com.pickupexpress.domain.vo.PickupOrderDetailVO;
import com.pickupexpress.domain.vo.PickupOrderVO;
import com.pickupexpress.mapper.*;
import com.pickupexpress.service.ContractService;
import com.pickupexpress.service.PickupOrderService;
import com.pickupexpress.service.ProgressEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 提货单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PickupOrderServiceImpl extends ServiceImpl<PickupOrderMapper, PickupOrder> implements PickupOrderService {

    private static final String PICKUP_NO_PREFIX = "TH";
    private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final ContractService contractService;
    private final ContractMapper contractMapper;
    private final PickupOrderMapper pickupOrderMapper;
    private final LiftRecordMapper liftRecordMapper;
    private final DeliveryConfirmMapper deliveryConfirmMapper;
    private final DeliveryPhotoMapper deliveryPhotoMapper;
    private final SettlementOrderMapper settlementOrderMapper;
    private final PickupVerificationMapper pickupVerificationMapper;
    private final EvidencePackageMapper evidencePackageMapper;
    private final ProgressEventService progressEventService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPickupOrder(DispatchRequestDTO dto) {
        Contract contract = contractService.getById(dto.getContractId());
        if (contract == null) {
            throw new BizException(ErrorCode.CONTRACT_NOT_FOUND);
        }
        TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());

        String pickupNo = generatePickupNo();
        String pickupCode = generatePickupCode();

        PickupOrder order = PickupOrder.builder()
                .pickupNo(pickupNo)
                .contractId(dto.getContractId())
                .contractNo(contract.getContractNo())
                .buyerId(contract.getBuyerId())
                .pickupCode(pickupCode)
                .pickupCodeStatus(PickupCodeStatusEnum.UNUSED.getValue())
                .dispatchMode(dto.getDispatchMode())
                .dispatchStatus(DispatchModeEnum.of(dto.getDispatchMode()) != null ? 1 : 0)
                .vehiclePlate(dto.getVehiclePlate())
                .driverName(dto.getDriverName())
                .driverPhone(dto.getDriverPhone())
                .carrierId(dto.getCarrierId())
                .carrierName(dto.getCarrierName())
                .expectedArrivalAt(dto.getExpectedArrivalAt())
                .warehouseId(contract.getWarehouseId())
                .warehouseName(contract.getWarehouseName())
                .totalLifts(0)
                .totalPieces(0)
                .totalWeight(java.math.BigDecimal.ZERO)
                .totalAmount(java.math.BigDecimal.ZERO)
                .deliveryStatus(0)
                .settlementStatus(0)
                .status(PickupOrderStatusEnum.DISPATCH_PENDING.getValue())
                .build();
        save(order);
        return order.getId();
    }

    private String generatePickupNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = pickupOrderMapper.selectCount(
                new LambdaQueryWrapper<PickupOrder>()
                        .likeRight(PickupOrder::getPickupNo, PICKUP_NO_PREFIX + dateStr));
        return PICKUP_NO_PREFIX + dateStr + String.format("%04d", count + 1);
    }

    @Override
    public String generatePickupCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    @Override
    public PickupOrderDetailVO getPickupOrderDetail(Long id) {
        PickupOrder order = getById(id);
        if (order == null) {
            throw new BizException(ErrorCode.PICKUP_ORDER_NOT_FOUND);
        }
        Contract contract = contractService.getById(order.getContractId());
        if (contract != null) {
            TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        }

        PickupOrderDetailVO vo = new PickupOrderDetailVO();
        BeanUtils.copyProperties(order, vo);
        vo.setLifts(liftRecordMapper.selectList(new LambdaQueryWrapper<com.pickupexpress.domain.entity.LiftRecord>().eq(com.pickupexpress.domain.entity.LiftRecord::getPickupOrderId, id)));
        vo.setDeliveryConfirm(deliveryConfirmMapper.selectOne(new LambdaQueryWrapper<com.pickupexpress.domain.entity.DeliveryConfirm>().eq(com.pickupexpress.domain.entity.DeliveryConfirm::getPickupOrderId, id)));
        vo.setPhotos(deliveryPhotoMapper.selectList(new LambdaQueryWrapper<com.pickupexpress.domain.entity.DeliveryPhoto>().eq(com.pickupexpress.domain.entity.DeliveryPhoto::getPickupOrderId, id)));
        vo.setSettlement(settlementOrderMapper.selectOne(new LambdaQueryWrapper<com.pickupexpress.domain.entity.SettlementOrder>().eq(com.pickupexpress.domain.entity.SettlementOrder::getPickupOrderId, id)));
        vo.setVerification(pickupVerificationMapper.selectOne(new LambdaQueryWrapper<com.pickupexpress.domain.entity.PickupVerification>().eq(com.pickupexpress.domain.entity.PickupVerification::getPickupOrderId, id)));
        if (order.getEvidencePackageId() != null) {
            vo.setEvidencePackage(evidencePackageMapper.selectById(order.getEvidencePackageId()));
        }
        return vo;
    }

    @Override
    public PageResult<PickupOrderVO> queryPickupOrders(Long contractId, Integer status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<PickupOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(contractId != null, PickupOrder::getContractId, contractId);
        wrapper.eq(status != null, PickupOrder::getStatus, status);
        Long currentEnterpriseId = com.pickupexpress.common.util.SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId != null) {
            List<Long> contractIds = contractService.list(
                    new LambdaQueryWrapper<Contract>()
                            .eq(Contract::getSellerId, currentEnterpriseId)
                            .or()
                            .eq(Contract::getBuyerId, currentEnterpriseId))
                    .stream().map(Contract::getId).toList();
            if (!contractIds.isEmpty()) {
                wrapper.in(PickupOrder::getContractId, contractIds);
            } else {
                wrapper.eq(PickupOrder::getId, -1);
            }
        }
        wrapper.orderByDesc(PickupOrder::getCreatedAt);

        int pn = pageNum != null && pageNum > 0 ? pageNum : 1;
        int ps = pageSize != null && pageSize > 0 ? pageSize : 20;
        IPage<PickupOrder> page = page(new Page<>(pn, ps), wrapper);

        List<PickupOrderVO> voList = page.getRecords().stream().map(o -> {
            PickupOrderVO vo = new PickupOrderVO();
            BeanUtils.copyProperties(o, vo);
            vo.setLifts(liftRecordMapper.selectList(new LambdaQueryWrapper<com.pickupexpress.domain.entity.LiftRecord>().eq(com.pickupexpress.domain.entity.LiftRecord::getPickupOrderId, o.getId())));
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, page.getTotal(), page.getSize(), page.getCurrent(), page.getPages());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDispatch(DispatchConfirmDTO dto) {
        PickupOrder order = getById(dto.getPickupOrderId());
        if (order == null) {
            throw new BizException(ErrorCode.PICKUP_ORDER_NOT_FOUND);
        }
        if (!dto.getConfirmed()) {
            order.setCustomerConfirmed(0);
            order.setCustomerConfirmedAt(null);
            order.setDispatchStatus(3);
            order.setStatus(PickupOrderStatusEnum.CANCELLED.getValue());
            updateById(order);
            progressEventService.recordEvent(order.getId(), order.getContractId(),
                    "DISPATCH_REJECTED", "派车被拒绝", null, "buyer");
            return;
        }
        order.setCustomerConfirmed(1);
        order.setCustomerConfirmedAt(LocalDateTime.now());
        order.setStatus(PickupOrderStatusEnum.READY.getValue());
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDriver(DriverAssignDTO dto) {
        PickupOrder order = getById(dto.getPickupOrderId());
        if (order == null) {
            throw new BizException(ErrorCode.PICKUP_ORDER_NOT_FOUND);
        }
        order.setDriverName(dto.getDriverName());
        order.setDriverPhone(dto.getDriverPhone());
        order.setVehiclePlate(dto.getVehiclePlate());
        order.setDriverAssigned(1);
        order.setDriverAssignedAt(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void driverAccept(Long pickupOrderId) {
        PickupOrder order = getById(pickupOrderId);
        if (order == null) {
            throw new BizException(ErrorCode.PICKUP_ORDER_NOT_FOUND);
        }
        order.setStatus(PickupOrderStatusEnum.ACCEPTED.getValue());
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void driverArrive(Long pickupOrderId, java.math.BigDecimal lat, java.math.BigDecimal lng) {
        PickupOrder order = getById(pickupOrderId);
        if (order == null) {
            throw new BizException(ErrorCode.PICKUP_ORDER_NOT_FOUND);
        }
        order.setStatus(PickupOrderStatusEnum.ARRIVED.getValue());
        order.setActualArrivalAt(LocalDateTime.now());
        order.setArrivalGpsLat(lat);
        order.setArrivalGpsLng(lng);
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPickupOrder(Long id) {
        PickupOrder order = getById(id);
        if (order == null) {
            throw new BizException(ErrorCode.PICKUP_ORDER_NOT_FOUND);
        }
        int status = order.getStatus() != null ? order.getStatus() : -1;
        if (status != PickupOrderStatusEnum.DISPATCH_PENDING.getValue() && status != PickupOrderStatusEnum.READY.getValue()) {
            throw new BizException(ErrorCode.CONTRACT_STATUS_ERROR);
        }
        order.setStatus(PickupOrderStatusEnum.CANCELLED.getValue());
        updateById(order);
    }

    @Override
    public List<PickupOrder> listByDriverPhone(String driverPhone) {
        if (driverPhone == null || driverPhone.isBlank()) {
            return new ArrayList<>();
        }
        return list(new LambdaQueryWrapper<PickupOrder>()
                .eq(PickupOrder::getDriverPhone, driverPhone)
                .orderByDesc(PickupOrder::getCreatedAt));
    }

    @Override
    public PickupOrder getByPickupCode(String pickupCode) {
        if (pickupCode == null || pickupCode.isBlank()) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<PickupOrder>().eq(PickupOrder::getPickupCode, pickupCode));
    }

    /**
     * 留货合同静默模式: 仓库发起时自动创建提货单
     * 仓库输入合同号+司机信息 → 系统匹配合同 → 自动生成提货单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPickupOrderFromWarehouse(Long contractId, String driverName, String driverPhone, String vehiclePlate) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BizException(ErrorCode.CONTRACT_NOT_FOUND);
        }
        TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());

        // Only for reserved contracts (静默模式)
        if (contract.getContractType() != ContractTypeEnum.RESERVED.getValue()) {
            throw new BizException(ErrorCode.CONTRACT_STATUS_ERROR.getCode(), "仅留货合同支持仓库直接发起");
        }

        // Contract must be READY or PICKING
        if (contract.getStatus() != ContractStatusEnum.READY.getValue()
                && contract.getStatus() != ContractStatusEnum.PICKING.getValue()) {
            throw new BizException(ErrorCode.CONTRACT_STATUS_ERROR.getCode(), "合同状态不允许提货");
        }

        // Auto-create pickup order (no dispatch needed for reserved contracts)
        String pickupNo = generatePickupNo();
        String pickupCode = generatePickupCode();

        PickupOrder order = PickupOrder.builder()
                .pickupNo(pickupNo)
                .contractId(contractId)
                .contractNo(contract.getContractNo())
                .buyerId(contract.getBuyerId())
                .pickupCode(pickupCode)
                .pickupCodeStatus(PickupCodeStatusEnum.VERIFIED.getValue())
                .pickupCodeExpireAt(LocalDateTime.now().plusHours(48))
                .dispatchMode(0)
                .dispatchStatus(2)
                .customerConfirmed(0)
                .vehiclePlate(vehiclePlate)
                .driverName(driverName)
                .driverPhone(driverPhone)
                .driverAssigned(1)
                .driverAssignedAt(LocalDateTime.now())
                .warehouseId(contract.getWarehouseId())
                .warehouseName(contract.getWarehouseName())
                .deliveryMode(2)
                .deliveryStatus(0)
                .settlementStatus(0)
                .totalLifts(0)
                .totalPieces(0)
                .totalWeight(java.math.BigDecimal.ZERO)
                .totalAmount(java.math.BigDecimal.ZERO)
                .status(PickupOrderStatusEnum.DELIVERING.getValue())
                .build();

        save(order);

        // Update contract status to PICKING
        if (contract.getStatus() == ContractStatusEnum.READY.getValue()) {
            contract.setStatus(ContractStatusEnum.PICKING.getValue());
            contractMapper.updateById(contract);
        }

        progressEventService.recordEvent(order.getId(), contractId,
                "PICKUP_ORDER_CREATED", "仓库发起提货(静默模式)", null, "warehouse");

        log.info("Warehouse-initiated pickup order: pickupNo={}, contractNo={}", pickupNo, contract.getContractNo());
        return order.getId();
    }
}
