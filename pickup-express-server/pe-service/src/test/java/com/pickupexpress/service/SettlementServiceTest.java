package com.pickupexpress.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pickupexpress.common.auth.DefaultAuthContext;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.LiftRecord;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.entity.SettlementOrder;
import com.pickupexpress.domain.enums.SettlementStatusEnum;
import com.pickupexpress.domain.vo.SettlementVO;
import com.pickupexpress.mapper.ContractMapper;
import com.pickupexpress.mapper.LiftRecordMapper;
import com.pickupexpress.mapper.PickupOrderMapper;
import com.pickupexpress.mapper.ContractItemMapper;
import com.pickupexpress.mapper.SettlementOrderMapper;
import com.pickupexpress.service.impl.SettlementServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private PickupOrderMapper pickupOrderMapper;

    @Mock
    private ContractMapper contractMapper;

    @Mock
    private LiftRecordMapper liftRecordMapper;

    @Mock
    private SettlementOrderMapper settlementOrderMapper;

    @Mock
    private ContractItemMapper contractItemMapper;

    @Mock
    private ProgressEventService progressEventService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SettlementServiceImpl settlementService;

    @BeforeEach
    void setUp() {
        DefaultAuthContext.setAuthInfo(1L, 1L, "test", "Test Enterprise", null);
        ReflectionTestUtils.setField(settlementService, "baseMapper", settlementOrderMapper);
    }

    @AfterEach
    void tearDown() {
        DefaultAuthContext.clearAuthInfo();
    }

    @Test
    void test_generateSettlement_success() {
        PickupOrder order = PickupOrder.builder()
                .id(1L)
                .contractId(1L)
                .contractNo("HT2024001")
                .buyerId(2L)
                .totalAmount(BigDecimal.valueOf(50000))
                .build();

        LiftRecord lift = LiftRecord.builder()
                .actualWeight(BigDecimal.valueOf(25.5))
                .theoreticalWeight(BigDecimal.valueOf(25.5))
                .build();

        when(pickupOrderMapper.selectById(1L)).thenReturn(order);
        when(contractMapper.selectById(1L)).thenReturn(Contract.builder().id(1L).sellerId(1L).buyerId(2L).build());
        when(liftRecordMapper.selectList(any())).thenReturn(List.of(lift));
        when(settlementOrderMapper.selectCount(any())).thenReturn(0L);
        doAnswer(inv -> {
            SettlementOrder s = inv.getArgument(0);
            s.setId(1L);
            return 1;
        }).when(settlementOrderMapper).insert(any(SettlementOrder.class));

        Long id = settlementService.generateSettlement(1L);

        assertNotNull(id);
        verify(settlementOrderMapper).insert(any(SettlementOrder.class));
        verify(pickupOrderMapper).updateById(argThat((PickupOrder o) ->
                o.getSettlementStatus().equals(SettlementStatusEnum.SETTLED.getValue())
        ));
    }

    @Test
    void test_markCustomerViewed_success() {
        SettlementOrder settlement = SettlementOrder.builder()
                .id(1L)
                .contractId(1L)
                .customerViewed(0)
                .build();

        when(settlementOrderMapper.selectById(1L)).thenReturn(settlement);
        when(contractMapper.selectById(1L)).thenReturn(Contract.builder().id(1L).sellerId(1L).buyerId(2L).build());

        settlementService.markCustomerViewed(1L);

        verify(settlementOrderMapper).updateById(argThat((SettlementOrder o) ->
                o.getCustomerViewed() == 1 && o.getCustomerViewedAt() != null
        ));
    }

    @Test
    void test_listByContract_success() {
        SettlementOrder s1 = SettlementOrder.builder().id(1L).contractId(1L).build();
        SettlementOrder s2 = SettlementOrder.builder().id(2L).contractId(1L).build();

        when(settlementOrderMapper.selectList(any())).thenReturn(List.of(s1, s2));

        List<SettlementOrder> list = settlementService.listByContract(1L);

        assertEquals(2, list.size());
        verify(settlementOrderMapper).selectList(any(LambdaQueryWrapper.class));
    }
}
