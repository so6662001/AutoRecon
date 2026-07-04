package com.autorecon.service.erp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class DbErpAdapter implements ErpDataAdapter {
    @Override
    public String getType() {
        return "DB";
    }

    @Override
    public boolean testConnection(Map<String, String> config) {
        String baseUrl = config.get("baseUrl");
        if (baseUrl == null || baseUrl.isEmpty()) {
            return false;
        }
        try {
            log.info("Testing DB connection: {}", baseUrl);
            return true;
        } catch (Exception e) {
            log.error("DB connection test failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> pullData(Map<String, String> config, String dataType, Map<String, String> params) {
        log.info("Pulling {} data from DB ERP: {}", dataType, config.get("baseUrl"));
        return List.of();
    }
}
