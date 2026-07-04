package com.autorecon.service.impl;

import com.autorecon.domain.dto.BillItemExcelDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Excel 列名与系统字段的智能匹配及数据标准化。
 */
@Service
@Slf4j
public class ExcelSmartMappingService {

    private static final Map<String, List<String>> FIELD_ALIASES = Map.ofEntries(
            Map.entry("contractNo", List.of("合同编号", "合同号", "合同", "contract")),
            Map.entry("orderNo", List.of("订单号", "订单编号", "订单", "order")),
            Map.entry("deliveryNo", List.of("发货单号", "提货单号", "发货编号", "delivery")),
            Map.entry("productName", List.of("品名", "产品名称", "产品", "品种", "商品名称", "货物名称", "名称", "product")),
            Map.entry("spec", List.of("规格", "规格型号", "型号", "规格/型号", "spec")),
            Map.entry("material", List.of("材质", "钢种", "材料", "material")),
            Map.entry("origin", List.of("产地", "钢厂", "生产厂家", "厂家", "origin")),
            Map.entry("quantity", List.of("数量", "件数", "支数", "根数", "qty", "quantity")),
            Map.entry("weight", List.of("重量", "吨数", "净重", "过磅重量", "实际重量", "发货吨数", "weight")),
            Map.entry("unitPrice", List.of("单价", "含税单价", "价格", "unit_price", "price")),
            Map.entry("amount", List.of("金额", "总金额", "货款", "含税金额", "amount")),
            Map.entry("deliveryDate", List.of("发货日期", "日期", "交货日期", "出库日期", "delivery_date", "date"))
    );

    public Map<String, List<String>> getFieldAliases() {
        return FIELD_ALIASES;
    }

    /**
     * 分析表头并返回「Excel 列名 → 系统字段」的映射建议。
     */
    public Map<String, String> analyzeHeaders(List<String> excelHeaders) {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String header : excelHeaders) {
            if (header == null || header.isEmpty()) {
                continue;
            }
            String trimmed = header.trim();
            String matched = matchField(trimmed);
            if (matched != null) {
                mapping.put(trimmed, matched);
            }
        }
        return mapping;
    }

    private String matchField(String header) {
        String normalized = normalizeForMatch(header);
        String bestField = null;
        int bestScore = -1;
        for (Map.Entry<String, List<String>> entry : FIELD_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                String na = normalizeForMatch(alias);
                int score = scoreMatch(normalized, na);
                if (score > bestScore) {
                    bestScore = score;
                    bestField = entry.getKey();
                }
            }
        }
        return bestScore > 0 ? bestField : null;
    }

    private static String normalizeForMatch(String s) {
        return s.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("　", "");
    }

    /** 分数越高越可信；避免过短的 contains 误匹配 */
    private static int scoreMatch(String normalizedHeader, String normalizedAlias) {
        if (normalizedAlias.isEmpty()) {
            return 0;
        }
        if (normalizedHeader.equals(normalizedAlias)) {
            return 100;
        }
        if (normalizedHeader.length() >= 2 && normalizedAlias.length() >= 2) {
            if (normalizedHeader.contains(normalizedAlias)) {
                return 50 + normalizedAlias.length();
            }
            if (normalizedAlias.contains(normalizedHeader)) {
                return 40 + normalizedHeader.length();
            }
        }
        return 0;
    }

    /**
     * 将原始行按映射转为 {@link BillItemExcelDTO}。
     */
    public List<BillItemExcelDTO> applyMapping(List<Map<String, String>> rawRows, Map<String, String> mapping) {
        List<BillItemExcelDTO> result = new ArrayList<>();
        if (rawRows == null || mapping == null) {
            return result;
        }
        for (Map<String, String> row : rawRows) {
            BillItemExcelDTO dto = new BillItemExcelDTO();
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                String excelCol = entry.getKey();
                String systemField = entry.getValue();
                if (systemField == null || systemField.isBlank()) {
                    continue;
                }
                String value = row.get(excelCol);
                if (value == null || value.isBlank()) {
                    continue;
                }
                setFieldValue(dto, systemField, value.trim());
            }
            result.add(dto);
        }
        return result;
    }

    private void setFieldValue(BillItemExcelDTO dto, String field, String value) {
        try {
            switch (field) {
                case "contractNo" -> dto.setContractNo(value);
                case "orderNo" -> dto.setOrderNo(value);
                case "deliveryNo" -> dto.setDeliveryNo(value);
                case "productName" -> dto.setProductName(value);
                case "spec" -> dto.setSpec(value);
                case "material" -> dto.setMaterial(value);
                case "origin" -> dto.setOrigin(value);
                case "quantity" -> dto.setQuantity(parseQuantity(value));
                case "weight" -> dto.setWeight(normalizeWeight(value));
                case "unitPrice" -> dto.setUnitPrice(new BigDecimal(cleanNumber(value)).setScale(4, RoundingMode.HALF_UP));
                case "amount" -> dto.setAmount(new BigDecimal(cleanNumber(value)).setScale(2, RoundingMode.HALF_UP));
                case "deliveryDate" -> dto.setDeliveryDate(parseDate(value));
                default -> log.debug("Unknown mapped field: {}", field);
            }
        } catch (Exception e) {
            log.warn("Failed to set field {} = {}: {}", field, value, e.getMessage());
        }
    }

    private BigDecimal parseQuantity(String value) {
        return new BigDecimal(cleanNumber(value)).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 重量标准化：支持 kg→吨。
     */
    private BigDecimal normalizeWeight(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        boolean isKg = lower.contains("kg");
        String cleaned = cleanNumber(
                value.replace("吨", "")
                        .replace("kg", "")
                        .replace("KG", "")
                        .replace("t", "")
                        .replace("T", "")
        );
        BigDecimal num = new BigDecimal(cleaned);
        if (isKg) {
            num = num.divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
        } else {
            num = num.setScale(4, RoundingMode.HALF_UP);
        }
        return num;
    }

    private String cleanNumber(String value) {
        if (value == null) {
            return "0";
        }
        return value.replace(",", "")
                .replace("，", "")
                .replace("¥", "")
                .replace("￥", "")
                .replace(" ", "")
                .trim();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] formats = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyyMMdd", "yyyy.MM.dd", "yyyy年MM月dd日"};
        for (String fmt : formats) {
            try {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern(fmt));
            } catch (Exception ignored) {
                // try next
            }
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
            return null;
        }
    }
}
