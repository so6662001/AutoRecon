package com.autorecon.service.impl;

import com.autorecon.common.util.TenantUtil;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.entity.ReconTemplate;
import com.autorecon.domain.vo.TemplatePreviewVO;
import com.autorecon.mapper.ReconTemplateMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TemplateRenderService {

    private final ReconTemplateMapper reconTemplateMapper;
    private final ObjectMapper objectMapper;

    /**
     * Preview template with sample data
     */
    public TemplatePreviewVO preview(Long templateId) {
        ReconTemplate template = reconTemplateMapper.selectById(templateId);
        if (template == null) {
            return null;
        }
        TenantUtil.checkOwnership(template.getEnterpriseId());

        TemplatePreviewVO vo = new TemplatePreviewVO();
        vo.setTemplateName(template.getTemplateName());
        vo.setTemplateType(template.getTemplateType());

        vo.setVisibleColumns(parseColumnConfig(template.getColumnConfig()));
        vo.setGroupBy(template.getGroupBy());
        vo.setSortBy(template.getSortBy());

        vo.setSampleItems(generateSampleItems());

        if ("CONTRACT".equals(template.getGroupBy())) {
            vo.setGroupedItems(groupByContract(vo.getSampleItems()));
            vo.setContractSummaries(calculateContractSummaries(vo.getGroupedItems()));
        }

        vo.setTotalSummary(calculateTotalSummary(vo.getSampleItems()));

        vo.setPaymentSummary(generateSamplePaymentSummary());

        return vo;
    }

    /**
     * Render a bill using its template config
     */
    public TemplatePreviewVO renderBill(ReconBill bill, List<ReconBillItem> items) {
        ReconTemplate template = null;
        if (bill.getTemplateId() != null) {
            template = reconTemplateMapper.selectById(bill.getTemplateId());
        }

        TemplatePreviewVO vo = new TemplatePreviewVO();
        vo.setTemplateName(template != null ? template.getTemplateName() : "默认模板");

        String groupBy = template != null ? template.getGroupBy() : null;
        String sortBy = template != null ? template.getSortBy() : null;

        List<ReconBillItem> workItems = items != null ? new ArrayList<>(items) : new ArrayList<>();

        if ("DATE".equals(sortBy)) {
            workItems.sort(Comparator.comparing(ReconBillItem::getDeliveryDate, Comparator.nullsLast(Comparator.naturalOrder())));
        } else if ("CONTRACT_NO".equals(sortBy)) {
            workItems.sort(Comparator.comparing(ReconBillItem::getContractNo, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        vo.setSampleItems(workItems);

        if ("CONTRACT".equals(groupBy)) {
            vo.setGroupedItems(groupByContract(workItems));
            vo.setContractSummaries(calculateContractSummaries(vo.getGroupedItems()));
        }

        vo.setTotalSummary(calculateTotalSummary(workItems));

        Map<String, Object> paymentSummary = new LinkedHashMap<>();
        paymentSummary.put("prevBalance", bill.getPrevBalance());
        paymentSummary.put("currentTradeAmount", bill.getCurrentTradeAmount());
        paymentSummary.put("currentPaymentAmount", bill.getCurrentPaymentAmount());
        paymentSummary.put("currentBalance", bill.getCurrentBalance());
        vo.setPaymentSummary(paymentSummary);

        return vo;
    }

    private List<String> parseColumnConfig(String columnConfig) {
        if (columnConfig == null || columnConfig.isEmpty()) {
            return List.of("contractNo", "productName", "spec", "material", "origin", "quantity", "weight", "unitPrice", "amount");
        }
        try {
            return objectMapper.readValue(columnConfig, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of("contractNo", "productName", "spec", "material", "quantity", "weight", "unitPrice", "amount");
        }
    }

    private Map<String, List<ReconBillItem>> groupByContract(List<ReconBillItem> items) {
        Map<String, List<ReconBillItem>> grouped = new LinkedHashMap<>();
        for (ReconBillItem item : items) {
            String key = item.getContractNo() != null && !item.getContractNo().isEmpty() ? item.getContractNo() : "(零星交易)";
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        return grouped;
    }

    private List<Map<String, Object>> calculateContractSummaries(Map<String, List<ReconBillItem>> grouped) {
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (Map.Entry<String, List<ReconBillItem>> entry : grouped.entrySet()) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("contractNo", entry.getKey());
            summary.put("itemCount", entry.getValue().size());
            BigDecimal totalWeight = entry.getValue().stream()
                    .map(i -> i.getWeight() != null ? i.getWeight() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalAmount = entry.getValue().stream()
                    .map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.put("totalWeight", totalWeight);
            summary.put("totalAmount", totalAmount);
            summaries.add(summary);
        }
        return summaries;
    }

    private Map<String, Object> calculateTotalSummary(List<ReconBillItem> items) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalQuantity", items.stream().map(i -> i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalWeight", items.stream().map(i -> i.getWeight() != null ? i.getWeight() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalAmount", items.stream().map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalTaxAmount", items.stream().map(i -> i.getTaxAmount() != null ? i.getTaxAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal totalWithTax = items.stream().map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("totalWithTax", totalWithTax);
        return summary;
    }

    private List<ReconBillItem> generateSampleItems() {
        List<ReconBillItem> items = new ArrayList<>();
        items.add(ReconBillItem.builder().contractNo("HT2026-023").contractName("2月螺纹钢采购").productName("螺纹钢").spec("Φ20").material("HRB400").origin("日照钢铁").quantity(new BigDecimal("100")).weight(new BigDecimal("98")).unitPrice(new BigDecimal("4200")).amount(new BigDecimal("411600")).build());
        items.add(ReconBillItem.builder().contractNo("HT2026-023").contractName("2月螺纹钢采购").productName("螺纹钢").spec("Φ25").material("HRB400").origin("日照钢铁").quantity(new BigDecimal("80")).weight(new BigDecimal("79")).unitPrice(new BigDecimal("4350")).amount(new BigDecimal("343650")).build());
        items.add(ReconBillItem.builder().contractNo("HT2026-025").contractName("盘螺框架").productName("盘螺").spec("Φ10").material("Q235").origin("沙钢").quantity(new BigDecimal("50")).weight(new BigDecimal("49")).unitPrice(new BigDecimal("4300")).amount(new BigDecimal("210700")).build());
        items.add(ReconBillItem.builder().productName("线材").spec("Φ6").material("Q195").origin("永钢").quantity(new BigDecimal("30")).weight(new BigDecimal("29")).unitPrice(new BigDecimal("3800")).amount(new BigDecimal("110200")).build());
        return items;
    }

    private Map<String, Object> generateSamplePaymentSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("prevBalance", new BigDecimal("50000"));
        summary.put("currentTradeAmount", new BigDecimal("1076150"));
        summary.put("totalPayable", new BigDecimal("1126150"));
        summary.put("payments", List.of(
                Map.of("date", "2026-02-08", "method", "银行转账", "amount", new BigDecimal("200000"), "bankRef", "BK00123"),
                Map.of("date", "2026-02-18", "method", "承兑汇票", "amount", new BigDecimal("300000"), "bankRef", "BK00456")
        ));
        summary.put("totalPaid", new BigDecimal("500000"));
        summary.put("currentBalance", new BigDecimal("626150"));
        return summary;
    }
}
