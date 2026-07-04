package com.pickupexpress.service;

import com.pickupexpress.common.auth.DefaultAuthContext;
import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.domain.dto.DeliveryCompleteDTO;
import com.pickupexpress.domain.dto.LiftUploadDTO;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.DeliveryConfirm;
import com.pickupexpress.domain.entity.LiftRecord;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.enums.PickupCodeStatusEnum;
import com.pickupexpress.mapper.ContractItemMapper;
import com.pickupexpress.mapper.ContractMapper;
import com.pickupexpress.mapper.DeliveryConfirmMapper;
import com.pickupexpress.mapper.DeliveryPhotoMapper;
import com.pickupexpress.mapper.LiftRecordMapper;
import com.pickupexpress.mapper.PickupOrderMapper;
import com.pickupexpress.service.impl.DeliveryServiceImpl;
import com.pickupexpress.service.ContractService;
import com.pickupexpress.service.ProgressEventService;
import com.pickupexpress.service.TradingHabitService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private PickupOrderMapper pickupOrderMapper;

    @Mock
    private ContractMapper contractMapper;

    @Mock
    private LiftRecordMapper liftRecordMapper;

    @Mock
    private ContractItemMapper contractItemMapper;

    @Mock
    private DeliveryConfirmMapper deliveryConfirmMapper;

    @Mock
    private DeliveryPhotoMapper deliveryPhotoMapper;

    @Mock
    private SettlementService settlementService;

    @Mock
    private ProgressEventService progressEventService;

    @Mock
    private ContractService contractService;

    @Mock
    private TradingHabitService tradingHabitService;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    @BeforeEach
    void setUp() {
        DefaultAuthContext.setAuthInfo(1L, 1L, "test", "Test Enterprise", null);
    }

    @AfterEach
    void tearDown() {
        DefaultAuthContext.clearAuthInfo();
    }

    private Contract testContract() {
        return Contract.builder().id(1L).sellerId(1L).buyerId(2L).build();
    }

    @Test
    void test_verifyPickupCode_valid_success() {
        PickupOrder order = PickupOrder.builder()
                .id(1L)
                .contractId(1L)
                .pickupCode("ABC123")
                .vehiclePlate("沪A12345")
                .pickupCodeExpireAt(LocalDateTime.now().plusHours(1))
                .build();

        when(pickupOrderMapper.selectOne(any())).thenReturn(order);
        when(contractMapper.selectById(1L)).thenReturn(testContract());

        boolean result = deliveryService.verifyPickupCode("ABC123", "沪A12345");

        assertTrue(result);
        verify(pickupOrderMapper).updateById(argThat((PickupOrder o) ->
                o.getPickupCodeStatus().equals(PickupCodeStatusEnum.VERIFIED.getValue())
        ));
    }

    @Test
    void test_verifyPickupCode_expired_throwsException() {
        PickupOrder order = PickupOrder.builder()
                .id(1L)
                .contractId(1L)
                .pickupCode("ABC123")
                .pickupCodeExpireAt(LocalDateTime.now().minusHours(1))
                .build();

        when(pickupOrderMapper.selectOne(any())).thenReturn(order);
        when(contractMapper.selectById(1L)).thenReturn(testContract());

        assertThrows(BizException.class, () -> deliveryService.verifyPickupCode("ABC123", "沪A12345"));
    }

    @Test
    void test_verifyPickupCode_wrongPlate_throwsException() {
        PickupOrder order = PickupOrder.builder()
                .id(1L)
                .contractId(1L)
                .pickupCode("ABC123")
                .vehiclePlate("沪A12345")
                .pickupCodeExpireAt(LocalDateTime.now().plusHours(1))
                .build();

        when(pickupOrderMapper.selectOne(any())).thenReturn(order);
        when(contractMapper.selectById(1L)).thenReturn(testContract());

        boolean result = deliveryService.verifyPickupCode("ABC123", "沪B99999");

        assertFalse(result);
        verify(pickupOrderMapper, never()).updateById(any(PickupOrder.class));
    }

    @Test
    void test_uploadLift_success() {
        PickupOrder order = PickupOrder.builder()
                .id(1L)
                .contractId(1L)
                .totalLifts(0)
                .totalPieces(0)
                .totalWeight(BigDecimal.ZERO)
                .build();

        when(contractMapper.selectById(1L)).thenReturn(testContract());

        LiftUploadDTO dto = new LiftUploadDTO();
        dto.setPickupOrderId(1L);
        dto.setProductName("螺纹钢Φ20 HRB400");
        dto.setSpec("Φ20");
        dto.setMaterial("HRB400");
        dto.setPieces(10);
        dto.setTheoreticalWeight(BigDecimal.valueOf(2.5));
        dto.setActualWeight(BigDecimal.valueOf(2.48));

        when(pickupOrderMapper.selectById(1L)).thenReturn(order);
        when(liftRecordMapper.selectCount(any())).thenReturn(0L);
        when(liftRecordMapper.selectList(any())).thenReturn(List.of(
                LiftRecord.builder()
                        .actualWeight(BigDecimal.valueOf(2.48))
                        .theoreticalWeight(BigDecimal.valueOf(2.5))
                        .build()
        ));

        deliveryService.uploadLift(dto);

        verify(liftRecordMapper).insert(argThat((LiftRecord l) -> l.getPickupOrderId() == 1L));
        verify(pickupOrderMapper).updateById(argThat((PickupOrder o) ->
                o.getTotalLifts() == 1 && o.getTotalWeight() != null
        ));
    }

    @Test
    void test_completeDelivery_success() {
        PickupOrder order = PickupOrder.builder()
                .id(1L)
                .contractId(1L)
                .contractNo("HT2024001")
                .buyerId(2L)
                .totalLifts(4)
                .totalPieces(40)
                .totalWeight(BigDecimal.valueOf(100))
                .build();

        DeliveryCompleteDTO dto = new DeliveryCompleteDTO();
        dto.setPickupOrderId(1L);
        dto.setOperatorId("OP001");
        dto.setOperatorName("操作员");
        dto.setSignatureUrl("https://example.com/sig.png");

        when(pickupOrderMapper.selectById(1L)).thenReturn(order);
        when(contractMapper.selectById(1L)).thenReturn(testContract());
        when(liftRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(contractItemMapper.selectList(any())).thenReturn(Collections.emptyList());

        deliveryService.completeDelivery(dto);

        verify(deliveryConfirmMapper).insert(argThat((DeliveryConfirm c) -> c.getPickupOrderId() == 1L));
        verify(pickupOrderMapper, atLeast(1)).updateById(any(PickupOrder.class));
        verify(settlementService).generateSettlement(1L);
        verify(tradingHabitService).recordPickup(eq(2L), isNull(), isNull(), isNull(),
                eq(BigDecimal.ZERO), isNull());
        verify(contractService).updatePickedAmount(eq(1L), eq(BigDecimal.ZERO), isNull());
    }
}
