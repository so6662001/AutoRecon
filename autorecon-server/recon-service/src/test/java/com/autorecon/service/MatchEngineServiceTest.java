package com.autorecon.service;

import com.autorecon.common.exception.BizException;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.vo.MatchResultVO;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.impl.MatchEngineServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchEngineServiceTest {

    @InjectMocks
    private MatchEngineServiceImpl matchEngineService;

    @Mock
    private ReconBillItemMapper reconBillItemMapper;

    @Mock
    private ReconBillMapper reconBillMapper;

    @Test
    void test_executeMatch_allMatched_buyerDataWithinTolerance() {
        ReconBill bill = ReconBill.builder().id(1L).build();
        ReconBillItem item = ReconBillItem.builder()
                .id(10L)
                .billId(1L)
                .weight(new BigDecimal("100.0"))
                .buyerWeight(new BigDecimal("100.2"))
                .quantity(new BigDecimal("10"))
                .buyerQuantity(new BigDecimal("10"))
                .totalAmount(new BigDecimal("10000"))
                .buyerAmount(new BigDecimal("10005"))
                .build();

        when(reconBillMapper.selectById(1L)).thenReturn(bill);
        when(reconBillItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

        MatchResultVO result = matchEngineService.executeMatch(1L);

        assertNotNull(result);
        assertEquals(1, result.getMatchedCount());
        assertEquals(0, result.getDiffCount());
        verify(reconBillItemMapper).updateById(any(ReconBillItem.class));
        verify(reconBillMapper).updateById(any(ReconBill.class));
    }

    @Test
    void test_executeMatch_weightDiffExceedsTolerance() {
        ReconBill bill = ReconBill.builder().id(1L).build();
        ReconBillItem item = ReconBillItem.builder()
                .id(10L)
                .billId(1L)
                .weight(new BigDecimal("100.0"))
                .buyerWeight(new BigDecimal("101.0"))
                .quantity(new BigDecimal("10"))
                .buyerQuantity(new BigDecimal("10"))
                .totalAmount(new BigDecimal("10000"))
                .buyerAmount(new BigDecimal("10000"))
                .build();

        when(reconBillMapper.selectById(1L)).thenReturn(bill);
        when(reconBillItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

        MatchResultVO result = matchEngineService.executeMatch(1L);

        assertEquals(0, result.getMatchedCount());
        assertEquals(1, result.getDiffCount());
    }

    @Test
    void test_executeMatch_sellerExtra_noBuyerData() {
        ReconBill bill = ReconBill.builder().id(1L).build();
        ReconBillItem item = ReconBillItem.builder()
                .id(10L)
                .billId(1L)
                .weight(new BigDecimal("100.0"))
                .quantity(new BigDecimal("10"))
                .totalAmount(new BigDecimal("10000"))
                .buyerQuantity(null)
                .buyerWeight(null)
                .buyerAmount(null)
                .build();

        when(reconBillMapper.selectById(1L)).thenReturn(bill);
        when(reconBillItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

        MatchResultVO result = matchEngineService.executeMatch(1L);

        assertEquals(0, result.getMatchedCount());
        assertEquals(1, result.getSellerExtraCount());
    }

    @Test
    void test_executeMatch_amountDiff() {
        ReconBill bill = ReconBill.builder().id(1L).build();
        ReconBillItem item = ReconBillItem.builder()
                .id(10L)
                .billId(1L)
                .weight(new BigDecimal("100.0"))
                .buyerWeight(new BigDecimal("100.0"))
                .quantity(new BigDecimal("10"))
                .buyerQuantity(new BigDecimal("10"))
                .totalAmount(new BigDecimal("10000"))
                .buyerAmount(new BigDecimal("10020"))
                .build();

        when(reconBillMapper.selectById(1L)).thenReturn(bill);
        when(reconBillItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

        MatchResultVO result = matchEngineService.executeMatch(1L);

        assertEquals(0, result.getMatchedCount());
        assertEquals(1, result.getDiffCount());
    }

    @Test
    void test_getDiffItems_returnsOnlyNonMatchedItems() {
        ReconBillItem matched = ReconBillItem.builder().id(1L).matchStatus(1).build();
        ReconBillItem diff = ReconBillItem.builder().id(2L).matchStatus(2).build();
        ReconBillItem sellerExtra = ReconBillItem.builder().id(3L).matchStatus(3).build();

        when(reconBillItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(matched, diff, sellerExtra));

        List<ReconBillItem> result = matchEngineService.getDiffItems(1L);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(i -> i.getMatchStatus() == 2));
        assertTrue(result.stream().anyMatch(i -> i.getMatchStatus() == 3));
    }

    @Test
    void test_executeMatch_billNotFound_throwsBizException() {
        when(reconBillMapper.selectById(999L)).thenReturn(null);

        assertThrows(BizException.class, () -> matchEngineService.executeMatch(999L));
    }
}
