package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.BatchCreateDTO;
import com.autorecon.domain.entity.Enterprise;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.vo.ReconBillVO;
import jakarta.validation.Valid;
import com.autorecon.mapper.EnterpriseMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.ReconBillService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 批量对账 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/batch")
@Tag(name = "批量对账")
@RequiredArgsConstructor
public class BatchReconController {

    private final ReconBillService reconBillService;
    private final ReconBillMapper reconBillMapper;
    private final EnterpriseMapper enterpriseMapper;

    @PostMapping("/create")
    @Operation(summary = "批量创建对账单")
    public R<String> batchCreate(@Valid @RequestBody BatchCreateDTO dto) {
        String batchId = reconBillService.batchCreateBills(dto.getBuyerIds(), dto.getPeriodStart(), dto.getPeriodEnd(), dto.getTemplateId());
        return R.ok(batchId);
    }

    @GetMapping("/{batchId}/progress")
    @Operation(summary = "查询批量对账进度")
    public R<Map<String, Object>> getBatchProgress(@PathVariable String batchId) {
        List<ReconBill> bills = reconBillMapper.selectList(
                new LambdaQueryWrapper<ReconBill>().eq(ReconBill::getBatchId, batchId));

        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId != null) {
            bills = bills.stream()
                    .filter(b -> currentEnterpriseId.equals(b.getSellerId()) || currentEnterpriseId.equals(b.getBuyerId()))
                    .collect(Collectors.toList());
        }

        int total = bills.size();
        long created = bills.stream().filter(b -> "CREATED".equals(b.getStatus())).count();
        long sent = bills.stream().filter(b -> !"CREATED".equals(b.getStatus()) && !"GENERATED".equals(b.getStatus())).count();
        long confirmed = bills.stream().filter(b -> "SIGNED".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus())).count();

        Map<String, Object> progress = new HashMap<>();
        progress.put("batchId", batchId);
        progress.put("total", total);
        progress.put("created", created);
        progress.put("sent", sent);
        progress.put("confirmed", confirmed);
        progress.put("completionRate", total > 0 ? (sent * 100 / total) : 0);
        return R.ok(progress);
    }

    @PostMapping("/{batchId}/remind")
    @Operation(summary = "一键催促未响应的买方")
    public R<Integer> remindUnresponded(@PathVariable String batchId) {
        List<ReconBill> pendingBills = reconBillMapper.selectList(
                new LambdaQueryWrapper<ReconBill>()
                        .eq(ReconBill::getBatchId, batchId)
                        .eq(ReconBill::getStatus, "PENDING"));

        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId != null) {
            pendingBills = pendingBills.stream()
                    .filter(b -> currentEnterpriseId.equals(b.getSellerId()))
                    .collect(Collectors.toList());
        }

        int reminded = 0;
        for (ReconBill bill : pendingBills) {
            // TODO: 发送催促通知给买方
            log.info("Remind buyer for bill: billNo={}, buyerId={}", bill.getBillNo(), bill.getBuyerId());
            reminded++;
        }
        return R.ok(reminded);
    }

    @GetMapping("/{batchId}/bills")
    @Operation(summary = "获取批量创建的对账单列表")
    public R<List<ReconBillVO>> getBatchBills(@PathVariable String batchId) {
        LambdaQueryWrapper<ReconBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconBill::getBatchId, batchId).orderByDesc(ReconBill::getCreatedAt);
        List<ReconBill> bills = reconBillMapper.selectList(wrapper);

        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId != null) {
            bills = bills.stream()
                    .filter(b -> currentEnterpriseId.equals(b.getSellerId()) || currentEnterpriseId.equals(b.getBuyerId()))
                    .collect(Collectors.toList());
        }

        List<Long> sellerIds = bills.stream().map(ReconBill::getSellerId).distinct().collect(Collectors.toList());
        List<Long> buyerIds = bills.stream().map(ReconBill::getBuyerId).distinct().collect(Collectors.toList());
        List<Long> allIds = new ArrayList<>();
        allIds.addAll(sellerIds);
        allIds.addAll(buyerIds);

        Map<Long, String> enterpriseNames = new HashMap<>();
        for (Long id : allIds) {
            if (id != null) {
                Enterprise e = enterpriseMapper.selectById(id);
                enterpriseNames.put(id, e != null ? e.getCompanyName() : "");
            }
        }

        List<ReconBillVO> voList = new ArrayList<>();
        for (ReconBill bill : bills) {
            ReconBillVO vo = new ReconBillVO();
            BeanUtils.copyProperties(bill, vo);
            vo.setSellerName(enterpriseNames.get(bill.getSellerId()));
            vo.setBuyerName(enterpriseNames.get(bill.getBuyerId()));
            voList.add(vo);
        }
        return R.ok(voList);
    }

    @Operation(summary = "批量发送对账单")
    @PostMapping("/{batchId}/send-all")
    public R<Integer> sendAllBills(@PathVariable String batchId) {
        List<ReconBill> bills = reconBillService.list(
                new LambdaQueryWrapper<ReconBill>()
                        .eq(ReconBill::getBatchId, batchId)
                        .in(ReconBill::getStatus, List.of("CREATED", "GENERATED")));
        int sent = 0;
        for (ReconBill bill : bills) {
            try {
                reconBillService.sendBill(bill.getId());
                sent++;
            } catch (Exception e) {
                log.warn("Failed to send bill {}: {}", bill.getId(), e.getMessage());
            }
        }
        return R.ok(sent);
    }
}
