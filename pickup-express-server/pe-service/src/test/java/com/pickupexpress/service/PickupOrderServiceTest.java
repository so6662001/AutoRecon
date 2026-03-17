package com.pickupexpress.service;

import com.pickupexpress.domain.dto.DispatchConfirmDTO;
import com.pickupexpress.domain.dto.DispatchRequestDTO;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.enums.DispatchModeEnum;
import com.pickupexpress.domain.enums.PickupOrderStatusEnum;
import com.pickupexpress.mapper.*;
import com.pickupexpress.service.impl.PickupOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PickupOrderServiceTest {

    @Mock
    private ContractService contractService;

    @Mock
    private PickupOrderMapper pickupOrderMapper;

    @Mock
    private LiftRecordMapper liftRecordMapper;

    @Mock
    private DeliveryConfirmMapper deliveryConfirmMapper;

    @Mock
    private DeliveryPhotoMapper deliveryPhotoMapper;

    @Mock
    private SettlementOrderMapper settlementOrderMapper;

    @Mock
    private PickupVerificationMapper pickupVerificationMapper;

    @Mock
    private EvidencePackageMapper evidencePackageMapper;

    @InjectMocks
    private PickupOrderServiceImpl pickupOrderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pickupOrderService, "baseMapper", pickupOrderMapper);
    }

    @Test
    void test_createPickupOrder_customerDispatch_success() {
        DispatchRequestDTO dto = new DispatchRequestDTO();
        dto.setContractId(1L);
        dto.setDispatchMode(DispatchModeEnum.CUSTOMER.getValue());
        dto.setVehiclePlate("沪A12345");
        dto.setDriverName("张三");
        dto.setDriverPhone("13800138000");

        Contract contract = Contract.builder()
                .id(1L)
                .contractNo("HT2024001")
                .buyerId(2L)
                .warehouseId(10L)
                .warehouseName("上海仓库")
                .build();

        when(contractService.getById(1L)).thenReturn(contract);
        when(pickupOrderMapper.selectCount(any())).thenReturn(0L);
        doAnswer(inv -> {
            PickupOrder o = inv.getArgument(0);
            o.setId(1L);
            return 1;
        }).when(pickupOrderMapper).insert(any(PickupOrder.class));

        Long id = pickupOrderService.createPickupOrder(dto);

        assertNotNull(id);
        assertEquals(1L, id);
        verify(pickupOrderMapper).insert(any(PickupOrder.class));
    }

    @Test
    void test_createPickupOrder_generatesPickupCode() {
        DispatchRequestDTO dto = new DispatchRequestDTO();
        dto.setContractId(1L);
        dto.setDispatchMode(DispatchModeEnum.CUSTOMER.getValue());

        Contract contract = Contract.builder()
                .id(1L)
                .contractNo("HT2024001")
                .buyerId(2L)
                .warehouseId(10L)
                .warehouseName("上海仓库")
                .build();

        when(contractService.getById(1L)).thenReturn(contract);
        when(pickupOrderMapper.selectCount(any())).thenReturn(0L);
        doAnswer(inv -> {
            PickupOrder o = inv.getArgument(0);
            o.setId(1L);
            return 1;
        }).when(pickupOrderMapper).insert(any(PickupOrder.class));

        Long id = pickupOrderService.createPickupOrder(dto);

        assertNotNull(id);
        ArgumentCaptor<PickupOrder> captor = ArgumentCaptor.forClass(PickupOrder.class);
        verify(pickupOrderMapper).insert(captor.capture());
        assertNotNull(captor.getValue().getPickupCode());
        assertEquals(6, captor.getValue().getPickupCode().length());
    }

    @Test
    void test_confirmDispatch_success() {
        PickupOrder order = PickupOrder.builder()
                .id(1L)
                .status(PickupOrderStatusEnum.DISPATCH_PENDING.getValue())
                .build();

        DispatchConfirmDTO dto = new DispatchConfirmDTO();
        dto.setPickupOrderId(1L);
        dto.setConfirmed(true);

        when(pickupOrderMapper.selectById(1L)).thenReturn(order);

        pickupOrderService.confirmDispatch(dto);

        verify(pickupOrderMapper).updateById(argThat((PickupOrder o) ->
                o.getCustomerConfirmed() == 1 && o.getStatus().equals(PickupOrderStatusEnum.READY.getValue())
        ));
    }

    @Test
    void test_driverAccept_success() {
        PickupOrder order = PickupOrder.builder()
                .id(1L)
                .status(PickupOrderStatusEnum.READY.getValue())
                .build();

        when(pickupOrderMapper.selectById(1L)).thenReturn(order);

        pickupOrderService.driverAccept(1L);

        verify(pickupOrderMapper).updateById(argThat((PickupOrder o) ->
                o.getStatus().equals(PickupOrderStatusEnum.ACCEPTED.getValue())
        ));
    }

    @Test
    void test_driverArrive_success() {
        PickupOrder order = PickupOrder.builder()
                .id(1L)
                .status(PickupOrderStatusEnum.ACCEPTED.getValue())
                .build();

        when(pickupOrderMapper.selectById(1L)).thenReturn(order);

        pickupOrderService.driverArrive(1L, java.math.BigDecimal.valueOf(31.23), java.math.BigDecimal.valueOf(121.47));

        verify(pickupOrderMapper).updateById(argThat((PickupOrder o) ->
                o.getStatus().equals(PickupOrderStatusEnum.ARRIVED.getValue())
                && o.getArrivalGpsLat() != null
                && o.getArrivalGpsLng() != null
        ));
    }
}
