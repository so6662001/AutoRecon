package com.autorecon.domain.vo;

import com.autorecon.domain.entity.ReconBillItem;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模板预览 / PDF 渲染视图数据
 */
@Data
public class TemplatePreviewVO {

    private String templateName;
    private Integer templateType;
    private List<String> visibleColumns;
    private String groupBy;
    private String sortBy;
    private List<ReconBillItem> sampleItems;
    private Map<String, List<ReconBillItem>> groupedItems;
    private List<Map<String, Object>> contractSummaries;
    private Map<String, Object> totalSummary;
    private Map<String, Object> paymentSummary;
}
