package com.pickupexpress.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pickupexpress.common.auth.DefaultAuthContext;
import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.common.result.PageResult;
import com.pickupexpress.domain.dto.ContractQueryDTO;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.ContractItem;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.enums.ContractStatusEnum;
import com.pickupexpress.domain.enums.ContractTypeEnum;
import com.pickupexpress.domain.enums.SignStatusEnum;
import com.pickupexpress.domain.vo.ContractDetailVO;
import com.pickupexpress.domain.vo.ContractVO;
import com.pickupexpress.mapper.ContractItemMapper;
import com.pickupexpress.mapper.ContractMapper;
import com.pickupexpress.mapper.PickupOrderMapper;
import com.pickupexpress.service.impl.ContractServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractMapper contractMapper;

    @Mock
    private ContractItemMapper contractItemMapper;

    @Mock
    private PickupOrderMapper pickupOrderMapper;

    @InjectMocks
    private ContractServiceImpl contractService;

    @BeforeEach
    void setUp() {
        DefaultAuthContext.setAuthInfo(1L, 1L, "test", "Test Enterprise", null);
        ReflectionTestUtils.setField(contractService, "baseMapper", contractMapper);
    }

    @Test
    void test_syncFromErp_reservedContract_statusReady() {
        Contract contract = Contract.builder()
                .contractNo("HT2024001")
                .contractType(ContractTypeEnum.RESERVED.getValue())
                .sellerId(1L)
                .buyerId(2L)
                .totalWeight(BigDecimal.valueOf(100))
                .totalAmount(BigDecimal.valueOf(50000))
                .build();
        List<ContractItem> items = Collections.emptyList();

        contractService.syncFromErp(contract, items);

        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractMapper, atLeast(1)).insert(any(Contract.class));
        verify(contractMapper).updateById(captor.capture());
        assertEquals(ContractStatusEnum.READY.getValue(), captor.getValue().getStatus());
    }

    @Test
    void test_syncFromErp_orderContract_statusPendingSign() {
        Contract contract = Contract.builder()
                .contractNo("HT2024002")
                .contractType(ContractTypeEnum.ORDER.getValue())
                .sellerId(1L)
                .buyerId(2L)
                .totalWeight(BigDecimal.valueOf(50))
                .totalAmount(BigDecimal.valueOf(25000))
                .build();
        ContractItem item = ContractItem.builder()
                .productName("螺纹钢Φ20 HRB400")
                .spec("Φ20")
                .material("HRB400")
                .weight(BigDecimal.valueOf(50))
                .amount(BigDecimal.valueOf(25000))
                .build();
        List<ContractItem> items = List.of(item);

        contractService.syncFromErp(contract, items);

        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractMapper).updateById(captor.capture());
        assertEquals(ContractStatusEnum.PENDING_SIGN.getValue(), captor.getValue().getStatus());
        verify(contractItemMapper).insert(any(ContractItem.class));
    }

    @Test
    void test_queryContracts_success() {
        ContractQueryDTO query = new ContractQueryDTO();
        query.setPageNum(1);
        query.setPageSize(20);

        Contract c = Contract.builder()
                .id(1L)
                .contractNo("HT2024001")
                .sellerId(1L)
                .buyerId(2L)
                .build();
        Page<Contract> page = new Page<>(1, 20);
        page.setRecords(List.of(c));
        page.setTotal(1);

        when(contractMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(contractItemMapper.selectCount(any())).thenReturn(2L);
        when(pickupOrderMapper.selectCount(any())).thenReturn(1L);

        PageResult<ContractVO> result = contractService.queryContracts(query);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals("HT2024001", result.getRecords().get(0).getContractNo());
    }

    @Test
    void test_getContractDetail_success() {
        Contract contract = Contract.builder()
                .id(1L)
                .contractNo("HT2024001")
                .sellerId(1L)
                .buyerId(2L)
                .build();
        ContractItem item = ContractItem.builder()
                .id(1L)
                .contractId(1L)
                .productName("螺纹钢")
                .build();

        when(contractMapper.selectById(1L)).thenReturn(contract);
        when(contractItemMapper.selectList(any())).thenReturn(List.of(item));

        ContractDetailVO vo = contractService.getContractDetail(1L);

        assertNotNull(vo);
        assertEquals("HT2024001", vo.getContractNo());
        assertEquals(1, vo.getItems().size());
    }

    @Test
    void test_initiateSign_success() {
        Contract contract = Contract.builder()
                .id(1L)
                .contractNo("HT2024001")
                .sellerId(1L)
                .buyerId(2L)
                .signStatus(SignStatusEnum.BUYER_SIGNED.getValue())
                .build();

        when(contractMapper.selectById(1L)).thenReturn(contract);

        contractService.initiateSign(1L);

        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractMapper).updateById(captor.capture());
        assertEquals(SignStatusEnum.NOT_SIGNED.getValue(), captor.getValue().getSignStatus());
    }

    @Test
    void test_customerSign_success() {
        Contract contract = Contract.builder()
                .id(1L)
                .contractNo("HT2024001")
                .sellerId(1L)
                .buyerId(2L)
                .signStatus(SignStatusEnum.NOT_SIGNED.getValue())
                .build();

        when(contractMapper.selectById(1L)).thenReturn(contract);

        contractService.customerSign(1L);

        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractMapper).updateById(captor.capture());
        assertEquals(SignStatusEnum.BUYER_SIGNED.getValue(), captor.getValue().getSignStatus());
    }
}
