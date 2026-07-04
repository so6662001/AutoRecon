package com.autorecon.service.erp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RestErpAdapter implements ErpDataAdapter {
    @Override
    public String getType() {
        return "REST";
    }

    @Override
    public boolean testConnection(Map<String, String> config) {
        String baseUrl = config.get("baseUrl");
        if (baseUrl == null || baseUrl.isEmpty()) {
            return false;
        }
        try {
            // Placeholder: In production, make HTTP call to baseUrl/health
            log.info("Testing REST connection to: {}", baseUrl);
            return true;
        } catch (Exception e) {
            log.error("REST connection test failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> pullData(Map<String, String> config, String dataType, Map<String, String> params) {
        log.info("Pulling {} data from REST ERP: {}", dataType, config.get("baseUrl"));
        // Placeholder: In production, call ERP REST API and return parsed data
        return List.of();
    }
}
