package com.pickupexpress.service;

import com.pickupexpress.common.auth.DefaultAuthContext;
import com.pickupexpress.domain.entity.*;
import com.pickupexpress.domain.vo.EvidencePackageVO;
import com.pickupexpress.mapper.*;
import com.pickupexpress.service.impl.EvidenceServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {

    @Mock
    private EvidencePackageMapper evidencePackageMapper;

    @Mock
    private PickupOrderMapper pickupOrderMapper;

    @Mock
    private ContractMapper contractMapper;

    @Mock
    private LiftRecordMapper liftRecordMapper;

    @Mock
    private DeliveryPhotoMapper deliveryPhotoMapper;

    @Mock
    private DeliveryConfirmMapper deliveryConfirmMapper;

    @Mock
    private SettlementOrderMapper settlementOrderMapper;

    @InjectMocks
    private EvidenceServiceImpl evidenceService;

    @BeforeEach
    void setUp() {
        DefaultAuthContext.setAuthInfo(1L, 1L, "test", "Test Enterprise", null);
        ReflectionTestUtils.setField(evidenceService, "baseMapper", evidencePackageMapper);
    }

    @AfterEach
    void tearDown() {
        DefaultAuthContext.clearAuthInfo();
    }

    @Test
    void test_archiveEvidence_success() {
        PickupOrder order = PickupOrder.builder()
                .id(1L)
                .contractId(1L)
                .pickupNo("TH202403170001")
                .pickupCode("ABC123")
                .build();

        Contract contract = Contract.builder()
                .id(1L)
                .contractNo("HT2024001")
                .sellerId(1L)
                .buyerId(2L)
                .signedPdfUrl("https://example.com/contract.pdf")
                .build();

        when(pickupOrderMapper.selectById(1L)).thenReturn(order);
        when(contractMapper.selectById(1L)).thenReturn(contract);
        when(liftRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(deliveryPhotoMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(deliveryConfirmMapper.selectOne(any())).thenReturn(null);
        when(settlementOrderMapper.selectOne(any())).thenReturn(null);
        doAnswer(inv -> {
            EvidencePackage p = inv.getArgument(0);
            p.setId(1L);
            return 1;
        }).when(evidencePackageMapper).insert(any(EvidencePackage.class));

        Long id = evidenceService.archiveEvidence(1L);

        assertNotNull(id);
        assertEquals(1L, id);
        verify(evidencePackageMapper).insert(any(EvidencePackage.class));
        verify(pickupOrderMapper).updateById(argThat((PickupOrder o) -> o.getEvidencePackageId() != null));
    }

    @Test
    void test_getEvidencePackage_success() {
        EvidencePackage pkg = EvidencePackage.builder()
                .id(1L)
                .pickupOrderId(1L)
                .contractId(1L)
                .packageHash("abc123")
                .build();

        PickupOrder order = PickupOrder.builder()
                .id(1L)
                .contractId(1L)
                .contractNo("HT2024001")
                .pickupNo("TH202403170001")
                .build();

        Contract contract = Contract.builder().id(1L).sellerId(1L).buyerId(2L).build();
        when(evidencePackageMapper.selectOne(any(), anyBoolean())).thenReturn(pkg);
        when(pickupOrderMapper.selectById(1L)).thenReturn(order);
        when(contractMapper.selectById(1L)).thenReturn(contract);

        EvidencePackageVO vo = evidenceService.getEvidencePackage(1L);

        assertNotNull(vo);
        assertEquals("HT2024001", vo.getContractNo());
        assertEquals("TH202403170001", vo.getPickupNo());
    }

    @Test
    void test_verifyIntegrity_success() throws Exception {
        String content = "HT001url1TH001ABC123" + "1" + "1" + "1";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String expectedHash = hexString.toString();

        EvidencePackage pkg = EvidencePackage.builder()
                .id(1L)
                .pickupOrderId(1L)
                .contractId(1L)
                .packageHash(expectedHash)
                .build();

        PickupOrder order = PickupOrder.builder()
                .id(1L)
                .contractId(1L)
                .pickupNo("TH001")
                .pickupCode("ABC123")
                .build();

        Contract contract = Contract.builder()
                .id(1L)
                .contractNo("HT001")
                .sellerId(1L)
                .buyerId(2L)
                .signedPdfUrl("url1")
                .build();

        LiftRecord lift = LiftRecord.builder()
                .id(1L)
                .actualWeight(BigDecimal.ONE)
                .theoreticalWeight(BigDecimal.ONE)
                .build();

        when(evidencePackageMapper.selectById(1L)).thenReturn(pkg);
        when(pickupOrderMapper.selectById(1L)).thenReturn(order);
        when(contractMapper.selectById(1L)).thenReturn(contract);
        when(liftRecordMapper.selectList(any())).thenReturn(List.of(lift));
        when(deliveryPhotoMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(deliveryConfirmMapper.selectOne(any())).thenReturn(null);
        when(settlementOrderMapper.selectOne(any())).thenReturn(null);

        boolean result = evidenceService.verifyIntegrity(1L);

        assertTrue(result);
    }
}
