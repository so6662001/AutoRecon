package com.autorecon.service;

import com.autorecon.common.auth.DefaultAuthContext;
import com.autorecon.common.exception.BizException;
import com.autorecon.domain.dto.DisputeCreateDTO;
import com.autorecon.domain.dto.DisputeMessageDTO;
import com.autorecon.domain.entity.Dispute;
import com.autorecon.domain.entity.DisputeMessage;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.enums.BillStatusEnum;
import com.autorecon.mapper.DisputeMapper;
import com.autorecon.mapper.DisputeMessageMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.impl.DisputeServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @InjectMocks
    private DisputeServiceImpl disputeService;

    @Mock
    private DisputeMapper disputeMapper;

    @Mock
    private DisputeMessageMapper disputeMessageMapper;

    @Mock
    private ReconBillMapper reconBillMapper;

    private static final Long TEST_ENTERPRISE_ID = 1L;

    @BeforeEach
    void setUp() {
        DefaultAuthContext.setAuthInfo(1L, TEST_ENTERPRISE_ID, "test", "Test", null);
    }

    @AfterEach
    void tearDown() {
        DefaultAuthContext.clearAuthInfo();
    }

    @Test
    void test_createDispute_success_billStatusChangesToDisputed() {

        DisputeCreateDTO dto = new DisputeCreateDTO();
        dto.setBillId(100L);
        dto.setBillItemId(200L);
        dto.setDisputeType(1);
        dto.setDescription("Test dispute");

        ReconBill bill = ReconBill.builder()
                .id(100L)
                .sellerId(TEST_ENTERPRISE_ID)
                .buyerId(2L)
                .status(BillStatusEnum.PENDING.getCode())
                .build();

        when(reconBillMapper.selectById(100L)).thenReturn(bill);
        doAnswer(inv -> {
            Dispute d = inv.getArgument(0);
            d.setId(500L);
            return null;
        }).when(disputeMapper).insert(any(Dispute.class));

        Long disputeId = disputeService.createDispute(dto);

        assertNotNull(disputeId);
        assertEquals(500L, disputeId);

        ArgumentCaptor<ReconBill> billCaptor = ArgumentCaptor.forClass(ReconBill.class);
        verify(reconBillMapper).updateById(billCaptor.capture());
        assertEquals(BillStatusEnum.DISPUTED.getCode(), billCaptor.getValue().getStatus());
    }

    @Test
    void test_resolveDispute_whenAllDisputesResolved_billGoesToToSign() {
        Dispute dispute = Dispute.builder().id(1L).billId(100L).status(0).build();
        ReconBill bill = ReconBill.builder()
                .id(100L)
                .sellerId(TEST_ENTERPRISE_ID)
                .buyerId(2L)
                .status(BillStatusEnum.DISPUTED.getCode())
                .build();

        when(disputeMapper.selectById(1L)).thenReturn(dispute);
        when(disputeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(reconBillMapper.selectById(100L)).thenReturn(bill);

        disputeService.resolveDispute(1L, "Resolved");

        ArgumentCaptor<ReconBill> billCaptor = ArgumentCaptor.forClass(ReconBill.class);
        verify(reconBillMapper).updateById(billCaptor.capture());
        assertEquals(BillStatusEnum.TO_SIGN.getCode(), billCaptor.getValue().getStatus());
    }

    @Test
    void test_resolveDispute_whenOpenDisputesRemain_billStaysDisputed() {
        Dispute dispute = Dispute.builder().id(1L).billId(100L).status(0).build();

        when(disputeMapper.selectById(1L)).thenReturn(dispute);
        when(disputeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        disputeService.resolveDispute(1L, "Resolved");

        verify(disputeMapper).updateById(any(Dispute.class));
        verify(reconBillMapper, never()).updateById(any(ReconBill.class));
    }

    @Test
    void test_sendMessage_createsMessageRecord() {

        DisputeMessageDTO dto = new DisputeMessageDTO();
        dto.setDisputeId(1L);
        dto.setMessageType(1);
        dto.setContent("Test message");

        Dispute dispute = Dispute.builder().id(1L).build();

        when(disputeMapper.selectById(1L)).thenReturn(dispute);

        disputeService.sendMessage(dto);

        ArgumentCaptor<DisputeMessage> captor = ArgumentCaptor.forClass(DisputeMessage.class);
        verify(disputeMessageMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getDisputeId());
        assertEquals("Test message", captor.getValue().getContent());
    }

    @Test
    void test_listDisputes_returnsDisputesForBill() {
        Dispute d1 = Dispute.builder().id(1L).billId(100L).build();
        Dispute d2 = Dispute.builder().id(2L).billId(100L).build();

        when(disputeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(d1, d2));

        List<Dispute> result = disputeService.listDisputes(100L);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }
}
