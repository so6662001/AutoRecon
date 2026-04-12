package com.autorecon.service.impl;

import com.autorecon.domain.dto.BillItemExcelDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExcelSmartMappingServiceTest {

    private final ExcelSmartMappingService service = new ExcelSmartMappingService();

    @Test
    void analyzeHeaders_mapsChineseAliases() {
        Map<String, String> m = service.analyzeHeaders(List.of("产品名称", "发货吨数", "规格型号"));
        assertEquals("productName", m.get("产品名称"));
        assertEquals("weight", m.get("发货吨数"));
        assertEquals("spec", m.get("规格型号"));
    }

    @Test
    void applyMapping_normalizesKgToTons() {
        List<Map<String, String>> rows = List.of(Map.of("重量", "500kg"));
        List<BillItemExcelDTO> dtos = service.applyMapping(rows, Map.of("重量", "weight"));
        assertNotNull(dtos.get(0).getWeight());
        assertEquals(0, new BigDecimal("0.5").compareTo(dtos.get(0).getWeight()));
    }
}
