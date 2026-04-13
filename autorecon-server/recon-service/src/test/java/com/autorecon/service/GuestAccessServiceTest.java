package com.autorecon.service;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.domain.dto.GuestConfirmDTO;
import com.autorecon.domain.entity.GuestAccessToken;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.mapper.DisputeMapper;
import com.autorecon.mapper.EnterpriseMapper;
import com.autorecon.mapper.GuestAccessTokenMapper;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.impl.GuestAccessServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestAccessServiceTest {

    @InjectMocks
    private GuestAccessServiceImpl guestAccessService;

    @Mock
    private GuestAccessTokenMapper guestAccessTokenMapper;

    @Mock
    private ReconBillMapper reconBillMapper;

    @Mock
    private ReconBillItemMapper reconBillItemMapper;

    @Mock
    private EnterpriseMapper enterpriseMapper;

    @Mock
    private DisputeMapper disputeMapper;

    @Mock
    private EngagementService engagementService;

    @Test
    void test_generateGuestToken_createsTokenWithCorrectExpiry() {
        ReconBill bill = ReconBill.builder().id(1L).build();

        when(reconBillMapper.selectById(1L)).thenReturn(bill);
        doAnswer(inv -> {
            GuestAccessToken t = inv.getArgument(0);
            t.setId(100L);
            return null;
        }).when(guestAccessTokenMapper).insert(any(GuestAccessToken.class));

        String token = guestAccessService.generateGuestToken(1L, "13800138000");

        assertNotNull(token);
        assertEquals(32, token.length());

        ArgumentCaptor<GuestAccessToken> captor = ArgumentCaptor.forClass(GuestAccessToken.class);
        verify(guestAccessTokenMapper).insert(captor.capture());
        assertNotNull(captor.getValue().getExpireAt());
        assertTrue(captor.getValue().getExpireAt().isAfter(LocalDateTime.now().plusDays(29)));
    }

    @Test
    void test_viewBill_expiredToken_throwsGuestTokenExpired() {
        GuestAccessToken token = GuestAccessToken.builder()
                .id(1L)
                .token("expired-token")
                .expireAt(LocalDateTime.now().minusDays(1))
                .build();

        when(guestAccessTokenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(token);

        BizException ex = assertThrows(BizException.class, () -> guestAccessService.viewBill("expired-token"));
        assertEquals(ErrorCode.GUEST_TOKEN_EXPIRED.getCode(), ex.getCode());
    }

    @Test
    void test_viewBill_invalidToken_throwsGuestTokenInvalid() {
        when(guestAccessTokenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> guestAccessService.viewBill("invalid-token"));
        assertEquals(ErrorCode.GUEST_TOKEN_INVALID.getCode(), ex.getCode());
    }

    @Test
    void test_guestConfirm_invalidToken_throwsGuestTokenInvalid() {
        GuestConfirmDTO dto = new GuestConfirmDTO();
        dto.setToken("invalid-token");
        dto.setConfirmed(true);

        when(guestAccessTokenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> guestAccessService.guestConfirm(dto));
        assertEquals(ErrorCode.GUEST_TOKEN_INVALID.getCode(), ex.getCode());
    }

    @Test
    void test_generateGuestToken_billNotFound_throwsBizException() {
        when(reconBillMapper.selectById(999L)).thenReturn(null);

        assertThrows(BizException.class, () -> guestAccessService.generateGuestToken(999L, "13800138000"));
    }
}
