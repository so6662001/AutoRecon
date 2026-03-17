package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.entity.ProgressEvent;
import com.pickupexpress.domain.entity.SettlementOrder;
import com.pickupexpress.domain.enums.ContractStatusEnum;
import com.pickupexpress.domain.enums.DeliveryStatusEnum;
import com.pickupexpress.domain.vo.DashboardVO;
import com.pickupexpress.domain.vo.ProgressEventSummaryVO;
import com.pickupexpress.mapper.ContractMapper;
import com.pickupexpress.mapper.PickupOrderMapper;
import com.pickupexpress.mapper.ProgressEventMapper;
import com.pickupexpress.mapper.SettlementOrderMapper;
import com.pickupexpress.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 仪表盘服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ContractMapper contractMapper;
    private final PickupOrderMapper pickupOrderMapper;
    private final SettlementOrderMapper settlementOrderMapper;
    private final ProgressEventMapper progressEventMapper;

    @Override
    public DashboardVO getDashboard(Long enterpriseId) {
        List<Contract> contracts = contractMapper.selectList(
                new LambdaQueryWrapper<Contract>().eq(Contract::getSellerId, enterpriseId));

        long contractReadyCount = contracts.stream().filter(c -> c.getStatus() != null && c.getStatus() == ContractStatusEnum.READY.getValue()).count();
        long contractPendingSignCount = contracts.stream().filter(c -> c.getStatus() != null && c.getStatus() == ContractStatusEnum.PENDING_SIGN.getValue()).count();
        long contractSignedCount = contracts.stream().filter(c -> c.getStatus() != null && c.getStatus() == ContractStatusEnum.SIGNED.getValue()).count();
        long contractPickingCount = contracts.stream().filter(c -> c.getStatus() != null && c.getStatus() == ContractStatusEnum.PICKING.getValue()).count();
        long contractPickedCount = contracts.stream().filter(c -> c.getStatus() != null && c.getStatus() == ContractStatusEnum.PICKED.getValue()).count();
        long contractSettledCount = contracts.stream().filter(c -> c.getStatus() != null && c.getStatus() == ContractStatusEnum.SETTLED.getValue()).count();

        List<Long> contractIds = contracts.stream().map(Contract::getId).toList();
        List<PickupOrder> pickups = contractIds.isEmpty() ? List.of() : pickupOrderMapper.selectList(
                new LambdaQueryWrapper<PickupOrder>().in(PickupOrder::getContractId, contractIds));

        long pickupNotStartedCount = pickups.stream().filter(p -> p.getDeliveryStatus() != null && p.getDeliveryStatus() == DeliveryStatusEnum.NOT_STARTED.getValue()).count();
        long pickupInProgressCount = pickups.stream().filter(p -> p.getDeliveryStatus() != null && p.getDeliveryStatus() == DeliveryStatusEnum.IN_PROGRESS.getValue()).count();
        long pickupCompletedCount = pickups.stream().filter(p -> p.getDeliveryStatus() != null && p.getDeliveryStatus() == DeliveryStatusEnum.COMPLETED.getValue()).count();

        List<Long> pickupOrderIds = pickups.stream().map(PickupOrder::getId).toList();
        BigDecimal totalReceivableAmount = BigDecimal.ZERO;
        if (!pickupOrderIds.isEmpty()) {
            List<SettlementOrder> settlements = settlementOrderMapper.selectList(
                    new LambdaQueryWrapper<SettlementOrder>().in(SettlementOrder::getPickupOrderId, pickupOrderIds));
            totalReceivableAmount = settlements.stream()
                    .map(SettlementOrder::getReceivableAmount)
                    .filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        List<ProgressEvent> recentEvents = contractIds.isEmpty() ? List.of() : progressEventMapper.selectList(
                new LambdaQueryWrapper<ProgressEvent>()
                        .in(ProgressEvent::getContractId, contractIds)
                        .orderByDesc(ProgressEvent::getCreatedAt)
                        .last("LIMIT 10"));

        List<ProgressEventSummaryVO> eventVOs = recentEvents.stream()
                .map(e -> ProgressEventSummaryVO.builder()
                        .id(e.getId())
                        .pickupOrderId(e.getPickupOrderId())
                        .contractId(e.getContractId())
                        .eventType(e.getEventType())
                        .eventTitle(e.getEventTitle())
                        .eventDetail(e.getEventDetail())
                        .operator(e.getOperator())
                        .createdAt(e.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return DashboardVO.builder()
                .contractReadyCount(contractReadyCount)
                .contractPendingSignCount(contractPendingSignCount)
                .contractSignedCount(contractSignedCount)
                .contractPickingCount(contractPickingCount)
                .contractPickedCount(contractPickedCount)
                .contractSettledCount(contractSettledCount)
                .pickupNotStartedCount(pickupNotStartedCount)
                .pickupInProgressCount(pickupInProgressCount)
                .pickupCompletedCount(pickupCompletedCount)
                .totalReceivableAmount(totalReceivableAmount)
                .recentEvents(eventVOs)
                .build();
    }
}
