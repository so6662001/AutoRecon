package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.common.exception.ErrorCode;
import com.pickupexpress.common.util.SecurityUtil;
import com.pickupexpress.common.util.TenantUtil;
import com.pickupexpress.domain.dto.SupplementCreateDTO;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.LiftRecord;
import com.pickupexpress.domain.entity.SupplementRecord;
import com.pickupexpress.domain.enums.ApprovalStatusEnum;
import com.pickupexpress.domain.enums.DataSourceEnum;
import com.pickupexpress.mapper.ContractMapper;
import com.pickupexpress.mapper.LiftRecordMapper;
import com.pickupexpress.mapper.PickupOrderMapper;
import com.pickupexpress.mapper.SupplementRecordMapper;
import com.pickupexpress.service.SupplementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 事后补录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class SupplementServiceImpl extends ServiceImpl<SupplementRecordMapper, SupplementRecord>
        implements SupplementService {

    private final ContractMapper contractMapper;
    private final PickupOrderMapper pickupOrderMapper;
    private final LiftRecordMapper liftRecordMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Long createSupplement(SupplementCreateDTO dto) {
        Contract contract = contractMapper.selectById(dto.getContractId());
        if (contract != null) {
            TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        }

        SupplementRecord record = SupplementRecord.builder()
                .pickupOrderId(dto.getPickupOrderId())
                .contractId(dto.getContractId())
                .supplementData(dto.getSupplementData())
                .documentUrls(dto.getDocumentUrls())
                .approvalStatus(ApprovalStatusEnum.PENDING.getValue())
                .build();

        save(record);
        return record.getId();
    }

    @Override
    public void approve(Long id, String comment) {
        SupplementRecord record = getById(id);
        if (record == null) return;

        Contract contract = contractMapper.selectById(record.getContractId());
        if (contract != null) {
            TenantUtil.checkOwnership(contract.getSellerId());
        }

        com.pickupexpress.domain.entity.PickupOrder order = pickupOrderMapper.selectById(record.getPickupOrderId());
        if (order != null && order.getDeliveryStatus() != null && order.getDeliveryStatus() == 2) {
            throw new BizException(ErrorCode.DELIVERY_ALREADY_COMPLETED);
        }

        record.setApprovalStatus(ApprovalStatusEnum.APPROVED.getValue());
        record.setApprovedAt(LocalDateTime.now());
        record.setApprovedBy(SecurityUtil.getCurrentUsername());
        updateById(record);

        processSupplementToLiftRecords(record);
    }

    @Override
    public void reject(Long id, String comment) {
        SupplementRecord record = getById(id);
        if (record == null) return;

        Contract contract = contractMapper.selectById(record.getContractId());
        if (contract != null) {
            TenantUtil.checkOwnership(contract.getSellerId());
        }

        record.setApprovalStatus(ApprovalStatusEnum.REJECTED.getValue());
        record.setRejectReason(comment);
        updateById(record);
    }

    @Override
    public List<SupplementRecord> listPending(Long enterpriseId) {
        List<Long> contractIds = contractMapper.selectList(
                new LambdaQueryWrapper<Contract>().eq(Contract::getSellerId, enterpriseId))
                .stream().map(Contract::getId).toList();

        if (contractIds.isEmpty()) {
            return List.of();
        }

        return list(new LambdaQueryWrapper<SupplementRecord>()
                .in(SupplementRecord::getContractId, contractIds)
                .eq(SupplementRecord::getApprovalStatus, ApprovalStatusEnum.PENDING.getValue())
                .orderByDesc(SupplementRecord::getCreatedAt));
    }

    @Override
    public void appendDocumentUrl(Long id, String fileUrl) {
        SupplementRecord record = getById(id);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "补录记录不存在");
        }
        Contract contract = contractMapper.selectById(record.getContractId());
        if (contract != null) {
            TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        }
        List<String> urls = new ArrayList<>();
        String existing = record.getDocumentUrls();
        if (existing != null && !existing.isBlank()) {
            try {
                urls.addAll(objectMapper.readValue(existing, new TypeReference<List<String>>() {}));
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse document_urls JSON, replacing with new list: {}", e.getMessage());
            }
        }
        urls.add(fileUrl);
        try {
            record.setDocumentUrls(objectMapper.writeValueAsString(urls));
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
        }
        updateById(record);
    }

    private void processSupplementToLiftRecords(SupplementRecord record) {
        if (record.getSupplementData() == null || record.getSupplementData().isBlank()) return;

        try {
            List<Map<String, Object>> items = objectMapper.readValue(record.getSupplementData(),
                    new TypeReference<List<Map<String, Object>>>() {});
            int seq = 1;
            for (Map<String, Object> item : items) {
                LiftRecord lift = LiftRecord.builder()
                        .pickupOrderId(record.getPickupOrderId())
                        .liftSeq(seq++)
                        .productName(getString(item, "productName"))
                        .spec(getString(item, "spec"))
                        .material(getString(item, "material"))
                        .heatNo(getString(item, "heatNo"))
                        .batchNo(getString(item, "batchNo"))
                        .pieces(getInt(item, "pieces"))
                        .theoreticalWeight(getBigDecimal(item, "theoreticalWeight"))
                        .actualWeight(getBigDecimal(item, "actualWeight"))
                        .dataSource(DataSourceEnum.SUPPLEMENT.getValue())
                        .build();
                liftRecordMapper.insert(lift);
            }
        } catch (Exception e) {
            log.warn("Failed to parse supplement data: {}", e.getMessage());
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private java.math.BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof java.math.BigDecimal) return (java.math.BigDecimal) v;
        try {
            return new java.math.BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
