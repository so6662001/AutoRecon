package com.autorecon.controller;

import com.autorecon.common.result.PageResult;
import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.InvoiceCreateDTO;
import com.autorecon.domain.dto.InvoiceLinkDTO;
import com.autorecon.domain.entity.Invoice;
import com.autorecon.domain.entity.InvoiceLink;
import com.autorecon.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 发票管理 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/invoices")
@Tag(name = "发票管理")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/")
    @Operation(summary = "创建发票")
    public R<Long> createInvoice(@Valid @RequestBody InvoiceCreateDTO dto) {
        Long id = invoiceService.createInvoice(dto);
        return R.ok(id);
    }

    @GetMapping("/")
    @Operation(summary = "发票列表")
    public R<PageResult<Invoice>> listInvoices(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        PageResult<Invoice> result = invoiceService.listInvoices(enterpriseId, pageNum, pageSize);
        return R.ok(result);
    }

    @PostMapping("/link")
    @Operation(summary = "关联发票到对账单明细")
    public R<Void> linkInvoiceToBillItem(@Valid @RequestBody InvoiceLinkDTO dto) {
        invoiceService.linkInvoiceToBillItem(dto);
        return R.ok();
    }

    @GetMapping("/links/{billId}")
    @Operation(summary = "获取对账单的发票关联")
    public R<List<InvoiceLink>> getInvoiceLinks(@PathVariable Long billId) {
        List<InvoiceLink> list = invoiceService.getInvoiceLinks(billId);
        return R.ok(list);
    }

    @DeleteMapping("/links/{linkId}")
    @Operation(summary = "解除发票关联")
    public R<Void> unlinkInvoice(@PathVariable Long linkId) {
        invoiceService.unlinkInvoice(linkId);
        return R.ok();
    }
}
