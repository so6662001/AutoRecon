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

        // ===== 按合同类型差异化处理 (设计文档4.3) =====
        int contractType = contract.getContractType() != null ? contract.getContractType() : 2;
        int dispatchMode = dto.getDispatchMode() != null ? dto.getDispatchMode() : 1;

        if (contractType == ContractTypeEnum.ORDER.getValue()) {
            // 订货合同: 必须已签约才能派车
            if (contract.getStatus() != ContractStatusEnum.SIGNED.getValue()
                    && contract.getStatus() != ContractStatusEnum.PICKING.getValue()) {
                throw new BizException(ErrorCode.CONTRACT_STATUS_ERROR.getCode(), "订货合同需签约后才能派车");
            }
        }

        // 校验合同可提货(已提完/已关闭则拒绝)
        if (contract.getStatus() == ContractStatusEnum.PICKED.getValue()
                || contract.getStatus() == ContractStatusEnum.SETTLED.getValue()
                || contract.getStatus() == ContractStatusEnum.CLOSED.getValue()) {
            throw new BizException(ErrorCode.CONTRACT_STATUS_ERROR.getCode(), "合同已提完或已关闭，无法派车");
        }

        // 客户自行派车(模式A): 自动审核通过→直接生成提货单
        // 销售代派(模式B): 待客户确认
        // 承运公司(模式C): 待分配驾驶员
        int initialStatus;
        int initialDispatchStatus;
        if (dispatchMode == DispatchModeEnum.CUSTOMER.getValue()) {
            // 模式A: 自动审核通过
            initialStatus = PickupOrderStatusEnum.READY.getValue();
            initialDispatchStatus = 2; // 已确认
        } else if (dispatchMode == DispatchModeEnum.SALES.getValue()) {
            // 模式B: 待客户确认
            initialStatus = PickupOrderStatusEnum.DISPATCH_PENDING.getValue();
            initialDispatchStatus = 1; // 待确认
        } else {
            // 模式C: 承运公司, 待分配驾驶员
            initialStatus = PickupOrderStatusEnum.DISPATCH_PENDING.getValue();
            initialDispatchStatus = 0; // 待分配
        }

        String pickupNo = generatePickupNo();
        String pickupCode = generatePickupCode();

        PickupOrder order = PickupOrder.builder()
                .pickupNo(pickupNo)
                .contractId(dto.getContractId())
                .contractNo(contract.getContractNo())
                .buyerId(contract.getBuyerId())
                .pickupCode(pickupCode)
                .pickupCodeStatus(PickupCodeStatusEnum.UNUSED.getValue())
                .pickupCodeExpireAt(LocalDateTime.now().plusHours(48)) // 48小时有效期
                .dispatchMode(dispatchMode)
                .dispatchStatus(initialDispatchStatus)
                .customerConfirmed(dispatchMode == DispatchModeEnum.CUSTOMER.getValue() ? 1 : 0)
                .vehiclePlate(dto.getVehiclePlate())
                .driverName(dto.getDriverName())
                .driverPhone(dto.getDriverPhone())
                .driverAssigned(dto.getDriverName() != null ? 1 : 0)
                .driverAssignedAt(dto.getDriverName() != null ? LocalDateTime.now() : null)
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
                .status(initialStatus)
                .build();
        save(order);

        // 记录进度事件
        String eventDetail = switch (dispatchMode) {
            case 1 -> "客户自行派车(自动审核通过)";
            case 2 -> "销售代派车(待客户确认)";
            case 3 -> "承运公司派车(待分配驾驶员)";
            default -> "派车申请";
        };
        progressEventService.recordEvent(order.getId(), contract.getId(),
                "DISPATCH_REQUESTED", eventDetail, null,
                dispatchMode == 1 ? "customer" : "seller");

        // 更新合同状态
        if (contract.getStatus() == ContractStatusEnum.READY.getValue()
                || contract.getStatus() == ContractStatusEnum.SIGNED.getValue()) {
            contract.setStatus(ContractStatusEnum.PICKING.getValue());
            contractMapper.updateById(contract);
        }

        // 模式B: 销售代派车→记录待确认通知(设计5.1: 派车待确认→客户)
        if (dispatchMode == DispatchModeEnum.SALES.getValue()) {
            progressEventService.recordEvent(order.getId(), contract.getId(),
                    "DISPATCH_PENDING_NOTIFICATION",
                    "已通知客户确认派车(车牌:" + order.getVehiclePlate() + ")",
                    null, "system");
            // TODO: 集成短信通知
        }

        // 提货码分发事件(设计文档4.4: 分发记录)
        if (order.getDriverName() != null && !order.getDriverName().isEmpty()) {
            progressEventService.recordEvent(order.getId(), contract.getId(),
                    "PICKUP_CODE_SENT",
                    "提货码已发送给驾驶员" + order.getDriverName() + "(码:" + pickupCode + "有效期48小时)",
                    null, "system");
        } else if (order.getCarrierName() != null && !order.getCarrierName().isEmpty()) {
            progressEventService.recordEvent(order.getId(), contract.getId(),
                    "PICKUP_CODE_SENT",
                    "提货码已发送给承运公司" + order.getCarrierName() + "(待分配驾驶员)",
                    null, "system");
        }

        log.info("Created pickup order: pickupNo={}, mode={}, status={}", pickupNo, eventDetail, initialStatus);
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
        order.setDispatchStatus(2); // 已确认
        order.setStatus(PickupOrderStatusEnum.READY.getValue());
        updateById(order);

        // 记录客户确认事件
        progressEventService.recordEvent(order.getId(), order.getContractId(),
                "DISPATCH_CONFIRMED", "客户确认派车", null, "buyer");
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
        order.setStatus(PickupOrderStatusEnum.READY.getValue()); // 分配后可提货
        updateById(order);

        // 记录驾驶员分配事件
        progressEventService.recordEvent(order.getId(), order.getContractId(),
                "DRIVER_ASSIGNED", "承运公司分配驾驶员: " + dto.getDriverName(), null, "carrier");
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

        // 证据: 驾驶员接单记录
        progressEventService.recordEvent(order.getId(), order.getContractId(),
                "DRIVER_ACCEPTED",
                "驾驶员" + (order.getDriverName() != null ? order.getDriverName() : "") + "已接单",
                null, order.getDriverName());
        log.info("Driver accepted: pickupOrderId={}, driver={}", pickupOrderId, order.getDriverName());
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

        // 证据: GPS到达记录(坐标+时间)
        String gpsInfo = lat != null && lng != null
                ? "(GPS:" + lat.toPlainString() + "," + lng.toPlainString() + ")"
                : "";
        progressEventService.recordEvent(order.getId(), order.getContractId(),
                "DRIVER_ARRIVED",
                "驾驶员已到达仓库" + (order.getWarehouseName() != null ? order.getWarehouseName() : "") + gpsInfo,
                null, order.getDriverName());
        log.info("Driver arrived: pickupOrderId={}, gps=({},{})", pickupOrderId, lat, lng);
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
