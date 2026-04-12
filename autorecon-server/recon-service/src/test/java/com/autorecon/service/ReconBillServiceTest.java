package com.autorecon.service;

import com.autorecon.common.auth.DefaultAuthContext;
import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.domain.dto.ReconBillCreateDTO;
import com.autorecon.domain.dto.ReconBillItemDTO;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.entity.ReconTemplate;
import com.autorecon.domain.enums.BillStatusEnum;
import com.autorecon.domain.vo.DashboardVO;
import com.autorecon.mapper.DisputeMapper;
import com.autorecon.mapper.EnterpriseMapper;
import com.autorecon.mapper.PaymentAllocationMapper;
import com.autorecon.mapper.PaymentMapper;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.mapper.ReconTemplateMapper;
import com.autorecon.service.impl.ReconBillServiceImpl;
import com.autorecon.service.impl.TemplateRenderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconBillServiceTest {

    @InjectMocks
    private ReconBillServiceImpl reconBillService;

    @Mock
    private ReconBillMapper reconBillMapper;

    @Mock
    private ReconBillItemMapper reconBillItemMapper;

    @Mock
    private ReconTemplateMapper reconTemplateMapper;

    @Mock
    private EnterpriseMapper enterpriseMapper;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentAllocationMapper paymentAllocationMapper;

    @Mock
    private DisputeMapper disputeMapper;

    @Mock
    private com.autorecon.common.config.AutoReconProperties autoReconProperties;

    @Mock
    private TemplateRenderService templateRenderService;

    private static final Long TEST_ENTERPRISE_ID = 1L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reconBillService, "baseMapper", reconBillMapper);
        DefaultAuthContext.setAuthInfo(1L, TEST_ENTERPRISE_ID, "test", "Test", null);
    }

    @AfterEach
    void tearDown() {
        DefaultAuthContext.clearAuthInfo();
    }

    @Test
    void test_createBill_success() {

        ReconBillCreateDTO dto = new ReconBillCreateDTO();
        dto.setBuyerId(2L);
        dto.setTemplateId(100L);
        dto.setPeriodStart(LocalDate.of(2025, 1, 1));
        dto.setPeriodEnd(LocalDate.of(2025, 1, 31));

        ReconBillItemDTO itemDto = new ReconBillItemDTO();
        itemDto.setAmount(new BigDecimal("1000"));
        itemDto.setQuantity(new BigDecimal("10"));
        itemDto.setWeight(new BigDecimal("100"));
        itemDto.setDeliveryDate(LocalDate.of(2025, 1, 15));
        itemDto.setSettleDate(LocalDate.of(2025, 1, 31));
        dto.setItems(List.of(itemDto));

        ReconTemplate template = ReconTemplate.builder()
                .id(100L)
                .templateName("Test Template")
                .build();

        when(reconTemplateMapper.selectById(100L)).thenReturn(template);
        doAnswer(inv -> {
            ReconBill bill = inv.getArgument(0);
            bill.setId(1000L);
            return null;
        }).when(reconBillMapper).insert(any(ReconBill.class));

        Long billId = reconBillService.createBill(dto);

        assertNotNull(billId);
        assertEquals(1000L, billId);

        ArgumentCaptor<ReconBill> billCaptor = ArgumentCaptor.forClass(ReconBill.class);
        verify(reconBillMapper).insert(billCaptor.capture());

        ReconBill capturedBill = billCaptor.getValue();
        assertTrue(capturedBill.getBillNo().startsWith("DZ"));
        assertEquals(new BigDecimal("1000"), capturedBill.getTotalAmount());
        assertEquals(BillStatusEnum.CREATED.getCode(), capturedBill.getStatus());

        verify(reconBillItemMapper, times(1)).insert(any(ReconBillItem.class));
    }

    @Test
    void test_createBill_missingTemplate_throwsBizException() {
        ReconBillCreateDTO dto = new ReconBillCreateDTO();
        dto.setBuyerId(2L);
        dto.setTemplateId(999L);
        dto.setPeriodStart(LocalDate.of(2025, 1, 1));
        dto.setPeriodEnd(LocalDate.of(2025, 1, 31));
        dto.setItems(List.of(new ReconBillItemDTO()));

        when(reconTemplateMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> reconBillService.createBill(dto));
        assertEquals(ErrorCode.TEMPLATE_NOT_FOUND.getCode(), ex.getCode());
        verify(reconBillMapper, never()).insert(any(ReconBill.class));
    }

    @Test
    void test_sendBill_success() {
        ReconBill bill = ReconBill.builder()
                .id(1L)
                .billNo("DZ001")
                .sellerId(TEST_ENTERPRISE_ID)
                .buyerId(2L)
                .status(BillStatusEnum.CREATED.getCode())
                .build();

        when(reconBillMapper.selectById(1L)).thenReturn(bill);
        com.autorecon.common.config.AutoReconProperties.AutoConfirm autoConfirm = new com.autorecon.common.config.AutoReconProperties.AutoConfirm();
        autoConfirm.setDefaultTimeoutDays(3);
        when(autoReconProperties.getAutoConfirm()).thenReturn(autoConfirm);

        reconBillService.sendBill(1L);

        ArgumentCaptor<ReconBill> captor = ArgumentCaptor.forClass(ReconBill.class);
        verify(reconBillMapper).updateById(captor.capture());
        assertEquals(BillStatusEnum.PENDING.getCode(), captor.getValue().getStatus());
    }

    @Test
    void test_sendBill_wrongStatus_throwsBizException() {
        ReconBill bill = ReconBill.builder()
                .id(1L)
                .sellerId(TEST_ENTERPRISE_ID)
                .buyerId(2L)
                .status(BillStatusEnum.SIGNED.getCode())
                .build();

        when(reconBillMapper.selectById(1L)).thenReturn(bill);

        BizException ex = assertThrows(BizException.class, () -> reconBillService.sendBill(1L));
        assertEquals(ErrorCode.BILL_STATUS_ERROR.getCode(), ex.getCode());
        verify(reconBillMapper, never()).updateById(any(ReconBill.class));
    }

    @Test
    void test_confirmBill_success() {
        ReconBill bill = ReconBill.builder()
                .id(1L)
                .sellerId(TEST_ENTERPRISE_ID)
                .buyerId(2L)
                .status(BillStatusEnum.PENDING.getCode())
                .build();

        when(reconBillMapper.selectById(1L)).thenReturn(bill);

        reconBillService.confirmBill(1L);

        ArgumentCaptor<ReconBill> captor = ArgumentCaptor.forClass(ReconBill.class);
        verify(reconBillMapper).updateById(captor.capture());
        assertEquals(BillStatusEnum.TO_SIGN.getCode(), captor.getValue().getStatus());
    }

    @Test
    void test_voidBill_success() {
        ReconBill bill = ReconBill.builder()
                .id(1L)
                .sellerId(TEST_ENTERPRISE_ID)
                .buyerId(2L)
                .status(BillStatusEnum.PENDING.getCode())
                .build();

        when(reconBillMapper.selectById(1L)).thenReturn(bill);

        reconBillService.voidBill(1L);

        ArgumentCaptor<ReconBill> captor = ArgumentCaptor.forClass(ReconBill.class);
        verify(reconBillMapper).updateById(captor.capture());
        assertEquals(BillStatusEnum.VOID.getCode(), captor.getValue().getStatus());
    }

    @Test
    void test_voidBill_afterSigned_throwsBizException() {
        ReconBill bill = ReconBill.builder()
                .id(1L)
                .sellerId(TEST_ENTERPRISE_ID)
                .buyerId(2L)
                .status(BillStatusEnum.SIGNED.getCode())
                .build();

        when(reconBillMapper.selectById(1L)).thenReturn(bill);

        BizException ex = assertThrows(BizException.class, () -> reconBillService.voidBill(1L));
        assertEquals(ErrorCode.BILL_STATUS_ERROR.getCode(), ex.getCode());
        verify(reconBillMapper, never()).updateById(any(ReconBill.class));
    }

    @Test
    void test_sendBill_billNotFound_throwsBizException() {
        when(reconBillMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> reconBillService.sendBill(999L));
        assertEquals(ErrorCode.BILL_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void test_getDashboard_countsBillsByStatus() {
        ReconBill pending = ReconBill.builder().id(1L).sellerId(10L).status(BillStatusEnum.PENDING.getCode())
                .currentBalance(new BigDecimal("100")).build();
        ReconBill disputed = ReconBill.builder().id(2L).sellerId(10L).status(BillStatusEnum.DISPUTED.getCode())
                .currentBalance(new BigDecimal("200")).build();
        ReconBill toSign = ReconBill.builder().id(3L).sellerId(10L).status(BillStatusEnum.TO_SIGN.getCode())
                .currentBalance(new BigDecimal("300")).build();

        when(reconBillMapper.selectList(any())).thenReturn(List.of(pending, disputed, toSign));

        DashboardVO vo = reconBillService.getDashboard(10L);

        assertEquals(1, vo.getPendingCount());
        assertEquals(1, vo.getDisputedCount());
        assertEquals(1, vo.getToSignCount());
        assertEquals(new BigDecimal("600"), vo.getTotalReceivable());
    }
}
