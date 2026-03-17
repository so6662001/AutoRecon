package com.pickupexpress.service;

import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.entity.PickupVerification;
import com.pickupexpress.domain.enums.VerificationLevelEnum;
import com.pickupexpress.domain.vo.VerificationResultVO;
import com.pickupexpress.mapper.PickupOrderMapper;
import com.pickupexpress.mapper.PickupVerificationMapper;
import com.pickupexpress.service.impl.PickupVerificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PickupVerificationServiceTest {

    @Mock
    private PickupOrderMapper pickupOrderMapper;

    @Mock
    private AuthorizedPickupPersonService authorizedPickupPersonService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PickupVerificationMapper pickupVerificationMapper;

    @InjectMocks
    private PickupVerificationServiceImpl pickupVerificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pickupVerificationService, "baseMapper", pickupVerificationMapper);
    }

    @Test
    void test_verify_preRegisteredDriver_standard() {
        PickupOrder order = PickupOrder.builder()
                .id(1L)
                .contractId(1L)
                .contractNo("HT2024001")
                .buyerId(2L)
                .driverName("张三")
                .driverPhone("13800138000")
                .vehiclePlate("沪A12345")
                .build();

        when(pickupOrderMapper.selectById(1L)).thenReturn(order);
        when(authorizedPickupPersonService.isAuthorized(2L, "13800138000")).thenReturn(true);
        doAnswer(inv -> {
            PickupVerification v = inv.getArgument(0);
            v.setId(1L);
            return 1;
        }).when(pickupVerificationMapper).insert(any(PickupVerification.class));

        VerificationResultVO result = pickupVerificationService.verify(1L, "张三", "13800138000", "沪A12345");

        assertNotNull(result);
        assertTrue(result.getIsPreRegistered());
        assertEquals("预登记司机，标准确权", result.getMessage());
        verify(notificationService).sendVerificationSms(eq(1L), eq("13800138000"), anyString());
    }

    @Test
    void test_verify_unknownDriver_enhanced() {
        PickupOrder order = PickupOrder.builder()
                .id(1L)
                .contractId(1L)
                .contractNo("HT2024001")
                .buyerId(2L)
                .driverName("李四")
                .driverPhone("13900139000")
                .vehiclePlate("沪B67890")
                .build();

        when(pickupOrderMapper.selectById(1L)).thenReturn(order);
        when(authorizedPickupPersonService.isAuthorized(2L, "13900139000")).thenReturn(false);
        doAnswer(inv -> {
            PickupVerification v = inv.getArgument(0);
            v.setId(1L);
            return 1;
        }).when(pickupVerificationMapper).insert(any(PickupVerification.class));

        VerificationResultVO result = pickupVerificationService.verify(1L, "李四", "13900139000", "沪B67890");

        assertNotNull(result);
        assertFalse(result.getIsPreRegistered());
        assertEquals("需加强确权", result.getMessage());
        assertTrue(result.getVerificationLevel() >= VerificationLevelEnum.ENHANCED.getValue());
    }

    @Test
    void test_sendVerificationSms_success() {
        PickupOrder order = PickupOrder.builder()
                .id(1L)
                .contractNo("HT2024001")
                .driverName("张三")
                .driverPhone("13800138000")
                .vehiclePlate("沪A12345")
                .build();

        when(pickupOrderMapper.selectById(1L)).thenReturn(order);

        pickupVerificationService.sendVerificationSms(1L);

        verify(notificationService).sendVerificationSms(eq(1L), eq("13800138000"), contains("HT2024001"));
    }

    @Test
    void test_recordPhoneCallResult_success() {
        PickupVerification verification = PickupVerification.builder()
                .id(1L)
                .phoneCallResult(0)
                .build();

        when(pickupVerificationMapper.selectById(1L)).thenReturn(verification);

        pickupVerificationService.recordPhoneCallResult(1L, 1, "https://example.com/recording.mp3");

        verify(pickupVerificationMapper).updateById(argThat((PickupVerification v) ->
                v.getPhoneCallResult() == 1 && "https://example.com/recording.mp3".equals(v.getPhoneCallRecordingUrl())
        ));
    }
}
