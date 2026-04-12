package com.pickupexpress.controller;

import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.common.exception.ErrorCode;
import com.pickupexpress.common.result.R;
import com.pickupexpress.common.util.SecurityUtil;
import com.pickupexpress.domain.dto.ErpContractItemDTO;
import com.pickupexpress.domain.dto.ErpContractSyncDTO;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.ContractItem;
import com.pickupexpress.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ERP 合同同步 HTTP 接口
 */
@RestController
@RequestMapping("/api/v1/evidence/erp")
@Tag(name = "ERP集成")
@RequiredArgsConstructor
@Slf4j
public class ErpSyncController {

    private final ContractService contractService;

    @Operation(summary = "接收ERP合同同步")
    @PostMapping("/contract/sync")
    public R<Long> syncContract(@RequestBody ErpContractSyncDTO dto) {
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        if (sellerId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        Contract contract = new Contract();
        contract.setContractNo(dto.getContractNo());
        contract.setErpContractId(dto.getErpContractId());
        contract.setContractType(dto.getContractType());
        contract.setSellerId(sellerId);
        contract.setBuyerId(null);
        contract.setBuyerContactName(dto.getBuyerContactName());
        contract.setBuyerContactPhone(dto.getBuyerContactPhone());
        contract.setTotalQuantity(dto.getTotalQuantity());
        contract.setTotalWeight(dto.getTotalWeight());
        contract.setTotalAmount(dto.getTotalAmount());
        contract.setPaymentTerms(dto.getPaymentTerms());
        contract.setDeliveryDeadline(dto.getDeliveryDeadline());
        contract.setWarehouseName(dto.getWarehouseName());
        contract.setErpSyncAt(LocalDateTime.now());
        if (dto.getBuyerName() != null) {
            contract.setCustomClauses("erpBuyerName=" + dto.getBuyerName());
        }

        List<ContractItem> items = new ArrayList<>();
        if (dto.getItems() != null) {
            for (ErpContractItemDTO row : dto.getItems()) {
                ContractItem item = new ContractItem();
                item.setProductName(row.getProductName());
                item.setSpec(row.getSpec());
                item.setMaterial(row.getMaterial());
                item.setOrigin(row.getOrigin());
                item.setQuantity(row.getQuantity());
                item.setWeight(row.getWeight());
                item.setUnitPrice(row.getUnitPrice());
                item.setAmount(row.getAmount());
                items.add(item);
            }
        }

        contractService.syncFromErp(contract, items);
        return R.ok(contract.getId());
    }

    @Operation(summary = "ERP合同状态回写回调")
    @PostMapping("/contract/callback")
    public R<Void> contractCallback(@RequestBody Map<String, Object> data) {
        log.info("Received ERP callback: {}", data);
        return R.ok();
    }
}
