package com.autorecon.domain.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 无固定表头模型的 Excel 读取：第一行为表头，数据行为列索引 → 单元格字符串。
 */
public class DynamicExcelListener extends AnalysisEventListener<Map<Integer, String>> {

    @Getter
    private List<String> headers = new ArrayList<>();
    @Getter
    private final List<Map<String, String>> dataList = new ArrayList<>();
    private boolean headerRead;

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        int maxIdx = headMap.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
        List<String> list = new ArrayList<>(Math.max(0, maxIdx + 1));
        for (int i = 0; i <= maxIdx; i++) {
            String v = headMap.get(i);
            list.add(v != null ? v.trim() : "");
        }
        headers = list;
        headerRead = true;
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        if (!headerRead || headers.isEmpty()) {
            return;
        }
        Map<String, String> row = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : data.entrySet()) {
            int idx = entry.getKey();
            if (idx >= 0 && idx < headers.size()) {
                String headerName = headers.get(idx);
                String val = entry.getValue();
                row.put(headerName, val != null ? val.trim() : "");
            }
        }
        if (row.values().stream().allMatch(v -> v == null || v.isEmpty())) {
            return;
        }
        dataList.add(row);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // no-op
    }
}
