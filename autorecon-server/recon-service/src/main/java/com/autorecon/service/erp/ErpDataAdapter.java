package com.autorecon.service.erp;

import java.util.List;
import java.util.Map;

public interface ErpDataAdapter {
    String getType(); // "REST", "WEBSERVICE", "DB", "FILE"

    boolean testConnection(Map<String, String> config);

    List<Map<String, Object>> pullData(Map<String, String> config, String dataType, Map<String, String> params);
}
