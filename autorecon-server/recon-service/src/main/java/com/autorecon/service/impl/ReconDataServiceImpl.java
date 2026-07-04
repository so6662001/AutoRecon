package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.TenantUtil;
import com.autorecon.domain.dto.BillItemExcelDTO;
import com.autorecon.domain.dto.ReconBillItemDTO;
import com.autorecon.domain.entity.BuyerDataConfig;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.listener.BillItemExcelListener;
import com.autorecon.domain.listener.DynamicExcelListener;
import com.autorecon.domain.vo.ExcelAnalysisVO;
import com.autorecon.mapper.BuyerDataConfigMapper;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.ReconDataService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.enums.ReadDefaultReturnEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 对账数据服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconDataServiceImpl implements ReconDataService {

    private static final int BUYER_DATA_SOURCE_EXCEL = 2;

    private final ReconBillMapper reconBillMapper;
    private final ReconBillItemMapper reconBillItemMapper;
    private final BuyerDataConfigMapper buyerDataConfigMapper;
    private final ExcelSmartMappingService excelSmartMappingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ReconBillItem> uploadExcel(Long billId, MultipartFile file) {
        ReconBill bill = reconBillMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());
        validateUploadFile(file);

        try {
            Map<String, String> savedMapping = getExcelMapping(bill.getBuyerId());
            List<BillItemExcelDTO> excelRows;
            if (savedMapping != null && !savedMapping.isEmpty()) {
                DynamicExcelListener dynamicListener = new DynamicExcelListener();
                try (InputStream is = file.getInputStream()) {
                    EasyExcel.read(is, dynamicListener)
                            .readDefaultReturn(ReadDefaultReturnEnum.STRING)
                            .sheet()
                            .doRead();
                }
                excelRows = excelSmartMappingService.applyMapping(dynamicListener.getDataList(), savedMapping);
                log.info("Uploaded Excel for billId={} using saved buyer mapping ({} columns), parsed {} rows",
                        billId, savedMapping.size(), excelRows.size());
            } else {
                BillItemExcelListener listener = new BillItemExcelListener();
                try (InputStream is = file.getInputStream()) {
                    EasyExcel.read(is, BillItemExcelDTO.class, listener).sheet().doRead();
                }
                excelRows = listener.getDataList();
                log.info("Uploaded Excel for billId={} using fixed @ExcelProperty template, parsed {} rows", billId, excelRows.size());
            }

            return processImportedRows(billId, excelRows);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Excel upload failed for billId={}", billId, e);
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Excel 解析失败，请检查文件格式是否正确");
        }
    }

    @Override
    public ExcelAnalysisVO analyzeExcelHeaders(MultipartFile file, Long buyerId) {
        validateUploadFile(file);
        try {
            DynamicExcelListener listener = new DynamicExcelListener();
            try (InputStream is = file.getInputStream()) {
                EasyExcel.read(is, listener)
                        .readDefaultReturn(ReadDefaultReturnEnum.STRING)
                        .sheet()
                        .doRead();
            }
            List<String> headers = new ArrayList<>(listener.getHeaders());
            Map<String, String> suggested = excelSmartMappingService.analyzeHeaders(headers);
            Map<String, String> saved = getExcelMapping(buyerId);

            List<String> unmapped = headers.stream()
                    .filter(h -> h != null && !h.isBlank() && !suggested.containsKey(h))
                    .collect(Collectors.toList());

            boolean autoMapped = !saved.isEmpty();
            if (autoMapped) {
                for (String h : headers) {
                    if (h == null || h.isBlank()) {
                        continue;
                    }
                    if (!saved.containsKey(h) || saved.get(h) == null || saved.get(h).isBlank()) {
                        autoMapped = false;
                        break;
                    }
                }
            }

            ExcelAnalysisVO vo = new ExcelAnalysisVO();
            vo.setHeaders(headers);
            vo.setSuggestedMapping(suggested);
            vo.setSavedMapping(saved.isEmpty() ? null : saved);
            vo.setAutoMapped(autoMapped);
            vo.setUnmappedHeaders(unmapped);
            vo.setSystemFields(excelSmartMappingService.getFieldAliases());
            return vo;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("analyzeExcelHeaders failed for buyerId={}", buyerId, e);
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Excel 表头解析失败，请检查文件格式是否正确");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ReconBillItem> uploadExcelWithMapping(Long billId, MultipartFile file, Map<String, String> mapping,
                                                      Boolean saveMapping, Long buyerId) {
        ReconBill bill = reconBillMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());
        validateUploadFile(file);

        Long resolvedBuyerId = buyerId != null ? buyerId : bill.getBuyerId();

        try {
            DynamicExcelListener listener = new DynamicExcelListener();
            try (InputStream is = file.getInputStream()) {
                EasyExcel.read(is, listener)
                        .readDefaultReturn(ReadDefaultReturnEnum.STRING)
                        .sheet()
                        .doRead();
            }

            Map<String, String> effectiveMapping = new LinkedHashMap<>();
            if (mapping != null && !mapping.isEmpty()) {
                effectiveMapping.putAll(mapping);
            } else {
                Map<String, String> saved = getExcelMapping(resolvedBuyerId);
                if (!saved.isEmpty()) {
                    effectiveMapping.putAll(saved);
                } else {
                    effectiveMapping.putAll(excelSmartMappingService.analyzeHeaders(listener.getHeaders()));
                }
            }

            List<BillItemExcelDTO> excelRows = excelSmartMappingService.applyMapping(listener.getDataList(), effectiveMapping);

            if (Boolean.TRUE.equals(saveMapping) && !effectiveMapping.isEmpty()) {
                saveExcelMapping(resolvedBuyerId, effectiveMapping);
            }

            log.info("uploadExcelWithMapping billId={}, buyerId={}, mappingSize={}, rows={}",
                    billId, resolvedBuyerId, effectiveMapping.size(), excelRows.size());
            return processImportedRows(billId, excelRows);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("uploadExcelWithMapping failed for billId={}", billId, e);
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Excel 解析失败，请检查文件格式是否正确");
        }
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "上传文件不能为空");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "文件大小不能超过10MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls") && !filename.toLowerCase().endsWith(".csv"))) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "仅支持 .xlsx、.xls、.csv 格式文件");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.contains("spreadsheet") && !contentType.contains("excel") && !"text/csv".equals(contentType) && !contentType.contains("octet-stream")) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "文件类型不正确");
        }
    }

    private List<ReconBillItem> processImportedRows(Long billId, List<BillItemExcelDTO> excelRows) {
        List<ReconBillItem> items = getBillItems(billId);
        List<ReconBillItem> parsedItems = new ArrayList<>();
        for (BillItemExcelDTO row : excelRows) {
            ReconBillItem matched = findMatchingItem(items, row.getDeliveryNo(), row.getOrderNo());
            if (matched != null) {
                matched.setBuyerQuantity(row.getQuantity());
                matched.setBuyerWeight(row.getWeight());
                matched.setBuyerAmount(row.getAmount() != null ? row.getAmount() : (row.getUnitPrice() != null && row.getQuantity() != null ? row.getUnitPrice().multiply(row.getQuantity()) : null));
                matched.setBuyerDataSource(BUYER_DATA_SOURCE_EXCEL);
                reconBillItemMapper.updateById(matched);
                parsedItems.add(matched);
            } else {
                ReconBillItem newItem = ReconBillItem.builder()
                        .billId(billId)
                        .lineNo(0)
                        .contractNo(row.getContractNo())
                        .orderNo(row.getOrderNo())
                        .deliveryNo(row.getDeliveryNo())
                        .productName(row.getProductName())
                        .spec(row.getSpec())
                        .material(row.getMaterial())
                        .origin(row.getOrigin())
                        .buyerQuantity(row.getQuantity())
                        .buyerWeight(row.getWeight())
                        .buyerAmount(row.getAmount())
                        .deliveryDate(row.getDeliveryDate())
                        .buyerDataSource(BUYER_DATA_SOURCE_EXCEL)
                        .build();
                reconBillItemMapper.insert(newItem);
                parsedItems.add(newItem);
            }
        }
        log.info("Processed imported rows for billId={}, {} items", billId, parsedItems.size());
        return parsedItems;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onlineSubmit(Long billId, List<ReconBillItemDTO> buyerItems) {
        ReconBill bill = reconBillMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());
        if (buyerItems == null) {
            return;
        }

        List<ReconBillItem> items = getBillItems(billId);
        for (ReconBillItemDTO dto : buyerItems) {
            ReconBillItem matched = findMatchingItem(items, dto.getDeliveryNo(), dto.getOrderNo());
            if (matched != null) {
                matched.setBuyerQuantity(dto.getQuantity());
                matched.setBuyerWeight(dto.getWeight());
                matched.setBuyerAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : (dto.getAmount() != null ? dto.getAmount() : null));
                reconBillItemMapper.updateById(matched);
            }
        }
        log.info("Online submit for billId={}, {} items", billId, buyerItems.size());
    }

    @Override
    public Map<String, String> getExcelMapping(Long buyerId) {
        LambdaQueryWrapper<BuyerDataConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuyerDataConfig::getEnterpriseId, buyerId);
        BuyerDataConfig config = buyerDataConfigMapper.selectOne(wrapper);
        if (config == null || config.getExcelMappingConfig() == null || config.getExcelMappingConfig().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(config.getExcelMappingConfig(), new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse excelMappingConfig for buyerId={}", buyerId, e);
            return new HashMap<>();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveExcelMapping(Long buyerId, Map<String, String> mapping) {
        LambdaQueryWrapper<BuyerDataConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuyerDataConfig::getEnterpriseId, buyerId);
        BuyerDataConfig config = buyerDataConfigMapper.selectOne(wrapper);
        try {
            String json = objectMapper.writeValueAsString(mapping != null ? mapping : new HashMap<>());
            if (config == null) {
                config = BuyerDataConfig.builder()
                        .enterpriseId(buyerId)
                        .submitMode(1)
                        .excelMappingConfig(json)
                        .build();
                buyerDataConfigMapper.insert(config);
            } else {
                config.setExcelMappingConfig(json);
                buyerDataConfigMapper.updateById(config);
            }
        } catch (Exception e) {
            log.error("Failed to save excel mapping for buyerId={}", buyerId, e);
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "保存映射配置失败");
        }
    }

    private List<ReconBillItem> getBillItems(Long billId) {
        LambdaQueryWrapper<ReconBillItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconBillItem::getBillId, billId);
        return reconBillItemMapper.selectList(wrapper);
    }

    private ReconBillItem findMatchingItem(List<ReconBillItem> items, String deliveryNo, String orderNo) {
        if (items == null) return null;
        for (ReconBillItem item : items) {
            if (deliveryNo != null && deliveryNo.equals(item.getDeliveryNo())) return item;
            if (orderNo != null && orderNo.equals(item.getOrderNo())) return item;
        }
        return null;
    }
}
