package com.autorecon.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Excel 智能映射分析结果
 */
@Data
public class ExcelAnalysisVO {

    /** Excel 原始表头（按列序） */
    private List<String> headers;
    /** 建议映射：Excel 列名 → 系统字段 */
    private Map<String, String> suggestedMapping;
    /** 已保存的映射（如有） */
    private Map<String, String> savedMapping;
    /** 已保存映射是否覆盖当前表头，可全自动应用 */
    private boolean autoMapped;
    /** 未能识别为系统字段的列名 */
    private List<String> unmappedHeaders;
    /** 系统支持的字段及其别名（供前端展示） */
    private Map<String, List<String>> systemFields;
}
