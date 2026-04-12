package com.autorecon.service.erp;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.common.util.TenantUtil;
import com.autorecon.domain.entity.ErpConnection;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.mapper.ErpConnectionMapper;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ErpPullService {

    private final ErpConnectionMapper erpConnectionMapper;
    private final ReconBillMapper reconBillMapper;
    private final ReconBillItemMapper reconBillItemMapper;
    private final ErpAdapterFactory adapterFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Pull trade data from ERP for a given connection and data type
     *
     * @param connectionId ERP connection config ID
     * @param dataType     one of: order, delivery, settlement, return, payment, invoice, contract
     * @param params       query parameters (e.g. periodStart, periodEnd, buyerId)
     * @return list of standardized data maps
     */
    public List<Map<String, Object>> pullData(Long connectionId, String dataType, Map<String, String> params) {
        ErpConnection conn = erpConnectionMapper.selectById(connectionId);
        if (conn == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "ERP connection not found");
        }
        assertConnectionOwnedByCurrentEnterprise(conn);

        ErpDataAdapter adapter = adapterFactory.getAdapter(conn.getConnectionType())
                .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST.getCode(), "No adapter for connection type: " + conn.getConnectionType()));

        // Build config map from connection
        Map<String, String> config = new HashMap<>();
        config.put("baseUrl", conn.getBaseUrl());
        config.put("authType", String.valueOf(conn.getAuthType()));
        config.put("authConfig", conn.getAuthConfig());

        // Pull raw data
        List<Map<String, Object>> rawData = adapter.pullData(config, dataType, params);

        // Apply field mapping if configured
        Map<String, String> fieldMapping = parseFieldMapping(conn.getFieldMapping());
        if (!fieldMapping.isEmpty()) {
            rawData = applyFieldMapping(rawData, fieldMapping);
        }

        log.info("Pulled {} records of type {} from ERP connection {}", rawData.size(), dataType, connectionId);
        return rawData;
    }

    /**
     * Test connection using the appropriate adapter
     */
    public boolean testConnection(Long id) {
        ErpConnection conn = erpConnectionMapper.selectById(id);
        if (conn == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "ERP连接不存在");
        }
        assertConnectionOwnedByCurrentEnterprise(conn);

        ErpDataAdapter adapter = adapterFactory.getAdapter(conn.getConnectionType()).orElse(null);
        if (adapter == null) {
            return false;
        }

        Map<String, String> config = new HashMap<>();
        config.put("baseUrl", conn.getBaseUrl());
        config.put("authType", String.valueOf(conn.getAuthType()));
        config.put("authConfig", conn.getAuthConfig());

        return adapter.testConnection(config);
    }

    /**
     * Pull from ERP and append rows to a recon bill (seller/buyer access enforced).
     */
    public int importFromErp(Long connectionId, Long billId, String dataType) {
        List<Map<String, Object>> data = pullData(connectionId, dataType, Map.of());
        List<ReconBillItem> items = convertToItems(data);
        ReconBill bill = reconBillMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());
        int nextLine = reconBillItemMapper.selectByBillId(billId).stream()
                .map(ReconBillItem::getLineNo)
                .max(Comparator.naturalOrder())
                .orElse(0);
        for (ReconBillItem item : items) {
            nextLine++;
            item.setBillId(billId);
            item.setLineNo(nextLine);
            reconBillItemMapper.insert(item);
        }
        return items.size();
    }

    private void assertConnectionOwnedByCurrentEnterprise(ErpConnection conn) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null || !enterpriseId.equals(conn.getEnterpriseId())) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * Apply field mapping: rename keys from ERP field names to system field names
     */
    private List<Map<String, Object>> applyFieldMapping(List<Map<String, Object>> rawData, Map<String, String> mapping) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rawData) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey();
                String mappedKey = mapping.getOrDefault(key, key);
                mapped.put(mappedKey, entry.getValue());
            }
            result.add(mapped);
        }
        return result;
    }

    private Map<String, String> parseFieldMapping(String fieldMappingJson) {
        if (fieldMappingJson == null || fieldMappingJson.isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(fieldMappingJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse field mapping: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * Convert pulled ERP data to ReconBillItems
     */
    public List<ReconBillItem> convertToItems(List<Map<String, Object>> data) {
        List<ReconBillItem> items = new ArrayList<>();
        for (Map<String, Object> row : data) {
            ReconBillItem item = ReconBillItem.builder()
                    .contractNo(getString(row, "contractNo"))
                    .orderNo(getString(row, "orderNo"))
                    .deliveryNo(getString(row, "deliveryNo"))
                    .productName(getString(row, "productName"))
                    .spec(getString(row, "spec"))
                    .material(getString(row, "material"))
                    .origin(getString(row, "origin"))
                    .quantity(getBigDecimal(row, "quantity"))
                    .weight(getBigDecimal(row, "weight"))
                    .unitPrice(getBigDecimal(row, "unitPrice"))
                    .amount(getBigDecimal(row, "amount"))
                    .build();
            items.add(item);
        }
        return items;
    }

    private String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) {
            return null;
        }
        try {
            return new BigDecimal(v.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
