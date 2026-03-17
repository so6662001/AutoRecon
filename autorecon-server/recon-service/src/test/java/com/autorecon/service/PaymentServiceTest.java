package com.autorecon.service;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.domain.dto.PaymentCreateDTO;
import com.autorecon.domain.entity.Payment;
import com.autorecon.domain.entity.PaymentAllocation;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.mapper.PaymentAllocationMapper;
import com.autorecon.mapper.PaymentMapper;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.impl.PaymentServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentAllocationMapper paymentAllocationMapper;

    @Mock
    private ReconBillItemMapper reconBillItemMapper;

    @Mock
    private ReconBillMapper reconBillMapper;

    @Test
    void test_createPayment_success() {
        PaymentCreateDTO dto = new PaymentCreateDTO();
        dto.setPayerId(1L);
        dto.setPayeeId(2L);
        dto.setPaymentDate(LocalDate.of(2025, 1, 15));
        dto.setPaymentAmount(new BigDecimal("5000"));
        dto.setPaymentMethod(1);

        doAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(100L);
            return null;
        }).when(paymentMapper).insert(any(Payment.class));

        Long paymentId = paymentService.createPayment(dto);

        assertNotNull(paymentId);
        assertEquals(100L, paymentId);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentMapper).insert(captor.capture());

        Payment captured = captor.getValue();
        assertTrue(captured.getPaymentNo().startsWith("PAY"));
        assertEquals(new BigDecimal("5000"), captured.getPaymentAmount());
        assertEquals(BigDecimal.ZERO, captured.getAllocatedAmount());
        assertEquals(new BigDecimal("5000"), captured.getUnallocatedAmount());
    }

    @Test
    void test_autoAllocateFIFO_allocatesInOrder() {
        Payment payment = Payment.builder()
                .id(1L)
                .payerId(1L)
                .payeeId(2L)
                .paymentAmount(new BigDecimal("250"))
                .allocatedAmount(BigDecimal.ZERO)
                .unallocatedAmount(new BigDecimal("250"))
                .build();

        ReconBill bill = ReconBill.builder().id(10L).sellerId(2L).buyerId(1L).status("PENDING").build();
        ReconBillItem item1 = ReconBillItem.builder().id(101L).billId(10L).unpaidAmount(new BigDecimal("100"))
                .deliveryDate(LocalDate.of(2025, 1, 1)).build();
        ReconBillItem item2 = ReconBillItem.builder().id(102L).billId(10L).unpaidAmount(new BigDecimal("200"))
                .deliveryDate(LocalDate.of(2025, 1, 2)).build();
        ReconBillItem item3 = ReconBillItem.builder().id(103L).billId(10L).unpaidAmount(new BigDecimal("300"))
                .deliveryDate(LocalDate.of(2025, 1, 3)).build();

        when(paymentMapper.selectById(1L)).thenReturn(payment);
        when(reconBillMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(bill));
        when(reconBillItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(item1, item2, item3));

        paymentService.autoAllocateFIFO(1L);

        verify(paymentAllocationMapper, times(2)).insert(any(PaymentAllocation.class));
        verify(reconBillItemMapper, times(2)).updateById(any(ReconBillItem.class));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentMapper).updateById(paymentCaptor.capture());
        assertEquals(new BigDecimal("250"), paymentCaptor.getValue().getAllocatedAmount());
        assertEquals(BigDecimal.ZERO, paymentCaptor.getValue().getUnallocatedAmount());
    }

    @Test
    void test_autoAllocateFIFO_paymentAlreadyFullyAllocated_throwsBizException() {
        Payment payment = Payment.builder()
                .id(1L)
                .unallocatedAmount(BigDecimal.ZERO)
                .build();

        when(paymentMapper.selectById(1L)).thenReturn(payment);

        BizException ex = assertThrows(BizException.class, () -> paymentService.autoAllocateFIFO(1L));
        assertEquals(ErrorCode.PAYMENT_ALREADY_ALLOCATED.getCode(), ex.getCode());
        verify(paymentAllocationMapper, never()).insert(any(PaymentAllocation.class));
    }

    @Test
    void test_getBalance_calculatesCorrectly() {
        ReconBill bill1 = ReconBill.builder().currentBalance(new BigDecimal("100")).build();
        ReconBill bill2 = ReconBill.builder().currentBalance(new BigDecimal("200")).build();

        when(reconBillMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(bill1, bill2));

        BigDecimal balance = paymentService.getBalance(2L, 1L);

        assertEquals(new BigDecimal("300"), balance);
    }
}
