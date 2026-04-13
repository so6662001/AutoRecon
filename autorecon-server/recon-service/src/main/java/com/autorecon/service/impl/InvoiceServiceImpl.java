package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.result.PageResult;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.common.util.TenantUtil;
import com.autorecon.domain.dto.InvoiceCreateDTO;
import com.autorecon.domain.dto.InvoiceLinkDTO;
import com.autorecon.domain.entity.Invoice;
import com.autorecon.domain.entity.InvoiceLink;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.mapper.InvoiceLinkMapper;
import com.autorecon.mapper.InvoiceMapper;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.InvoiceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发票服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice> implements InvoiceService {

    private final InvoiceMapper invoiceMapper;
    private final InvoiceLinkMapper invoiceLinkMapper;
    private final ReconBillMapper reconBillMapper;
    private final ReconBillItemMapper reconBillItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInvoice(InvoiceCreateDTO dto) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        Invoice invoice = Invoice.builder()
                .enterpriseId(enterpriseId)
                .invoiceNo(dto.getInvoiceNo())
                .invoiceCode(dto.getInvoiceCode())
                .invoiceType(dto.getInvoiceType())
                .amount(dto.getAmount())
                .taxAmount(dto.getTaxAmount())
                .totalAmount(dto.getTotalAmount())
                .invoiceDate(dto.getInvoiceDate())
                .buyerName(dto.getBuyerName())
                .sellerName(dto.getSellerName())
                .status(1)
                .ocrRecognized(0)
                .build();

        invoiceMapper.insert(invoice);
        log.info("Created invoice: id={}, invoiceNo={}", invoice.getId(), dto.getInvoiceNo());
        return invoice.getId();
    }

    @Override
    public PageResult<Invoice> listInvoices(Long enterpriseId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Invoice::getEnterpriseId, enterpriseId)
                .orderByDesc(Invoice::getInvoiceDate);

        int pn = pageNum != null && pageNum > 0 ? pageNum : 1;
        int ps = pageSize != null && pageSize > 0 ? pageSize : 10;
        Page<Invoice> page = new Page<>(pn, ps);
        IPage<Invoice> result = invoiceMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkInvoiceToBillItem(InvoiceLinkDTO dto) {
        Invoice invoice = invoiceMapper.selectById(dto.getInvoiceId());
        if (invoice == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "发票不存在");
        }
        ReconBill bill = reconBillMapper.selectById(dto.getBillId());
        if (bill != null) {
            TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());
        }

        InvoiceLink link = InvoiceLink.builder()
                .billId(dto.getBillId())
                .billItemId(dto.getBillItemId())
                .invoiceId(dto.getInvoiceId())
                .invoiceNo(invoice.getInvoiceNo())
                .invoiceCode(invoice.getInvoiceCode())
                .invoiceType(invoice.getInvoiceType())
                .invoiceAmount(invoice.getAmount())
                .taxAmount(invoice.getTaxAmount())
                .invoiceDate(invoice.getInvoiceDate())
                .linkAmount(dto.getLinkAmount())
                .build();

        invoiceLinkMapper.insert(link);

        // 更新明细行开票状态
        if (dto.getBillItemId() != null) {
            updateItemInvoiceStatus(dto.getBillItemId());
        }

        log.info("Linked invoice to bill: billId={}, invoiceId={}, linkAmount={}", dto.getBillId(), dto.getInvoiceId(), dto.getLinkAmount());
    }

    /**
     * 更新明细行的开票状态: 0-未开票, 1-已开票, 2-部分开票
     */
    private void updateItemInvoiceStatus(Long billItemId) {
        ReconBillItem item = reconBillItemMapper.selectById(billItemId);
        if (item == null) return;

        // 查询该明细行已关联的发票总金额
        List<InvoiceLink> links = invoiceLinkMapper.selectList(
                new LambdaQueryWrapper<InvoiceLink>().eq(InvoiceLink::getBillItemId, billItemId));
        BigDecimal totalLinked = links.stream()
                .map(l -> l.getLinkAmount() != null ? l.getLinkAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal itemAmount = item.getTotalAmount() != null ? item.getTotalAmount()
                : (item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);

        if (totalLinked.compareTo(BigDecimal.ZERO) == 0) {
            item.setInvoiceStatus(0); // 未开票
        } else if (totalLinked.compareTo(itemAmount) >= 0) {
            item.setInvoiceStatus(1); // 已开票
        } else {
            item.setInvoiceStatus(2); // 部分开票
        }

        item.setInvoiceNo(links.isEmpty() ? null : links.get(0).getInvoiceNo());
        reconBillItemMapper.updateById(item);
    }

    @Override
    public List<InvoiceLink> getInvoiceLinks(Long billId) {
        ReconBill bill = reconBillMapper.selectById(billId);
        if (bill != null) {
            TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());
        }
        LambdaQueryWrapper<InvoiceLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvoiceLink::getBillId, billId).orderByDesc(InvoiceLink::getCreatedAt);
        return invoiceLinkMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlinkInvoice(Long linkId) {
        InvoiceLink link = invoiceLinkMapper.selectById(linkId);
        if (link == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "关联记录不存在");
        }
        Long billItemId = link.getBillItemId();
        invoiceLinkMapper.deleteById(linkId);

        // 取消关联后刷新明细行开票状态
        if (billItemId != null) {
            updateItemInvoiceStatus(billItemId);
        }

        log.info("Unlinked invoice: linkId={}", linkId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoLinkInvoices(Long billId) {
        ReconBill bill = reconBillMapper.selectById(billId);
        if (bill == null) throw new BizException(ErrorCode.BILL_NOT_FOUND);
        TenantUtil.checkBillAccess(bill.getSellerId(), bill.getBuyerId());

        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) return 0;

        // 获取对账单所有明细行
        List<ReconBillItem> items = reconBillItemMapper.selectList(
                new LambdaQueryWrapper<ReconBillItem>().eq(ReconBillItem::getBillId, billId));

        // 获取企业所有未关联的发票
        List<Invoice> allInvoices = invoiceMapper.selectList(
                new LambdaQueryWrapper<Invoice>()
                        .eq(Invoice::getEnterpriseId, enterpriseId)
                        .eq(Invoice::getDeleted, 0));

        // 已关联的发票ID集合
        List<InvoiceLink> existingLinks = invoiceLinkMapper.selectList(
                new LambdaQueryWrapper<InvoiceLink>().eq(InvoiceLink::getBillId, billId));
        java.util.Set<Long> linkedInvoiceIds = existingLinks.stream()
                .map(InvoiceLink::getInvoiceId)
                .collect(java.util.stream.Collectors.toSet());

        int linked = 0;
        for (Invoice invoice : allInvoices) {
            if (linkedInvoiceIds.contains(invoice.getId())) continue;

            // 尝试按买方名称匹配
            if (invoice.getBuyerName() == null) continue;

            for (ReconBillItem item : items) {
                // 按合同号匹配: 发票备注/买方名与明细合同号
                boolean matched = false;

                // 策略1: 金额精确匹配(发票金额 = 明细金额)
                if (invoice.getAmount() != null && item.getAmount() != null
                        && invoice.getAmount().compareTo(item.getAmount()) == 0) {
                    matched = true;
                }

                // 策略2: 发票总金额 = 明细价税合计
                if (!matched && invoice.getTotalAmount() != null && item.getTotalAmount() != null
                        && invoice.getTotalAmount().compareTo(item.getTotalAmount()) == 0) {
                    matched = true;
                }

                if (matched) {
                    InvoiceLink link = InvoiceLink.builder()
                            .billId(billId)
                            .billItemId(item.getId())
                            .invoiceId(invoice.getId())
                            .invoiceNo(invoice.getInvoiceNo())
                            .invoiceCode(invoice.getInvoiceCode())
                            .invoiceType(invoice.getInvoiceType())
                            .invoiceAmount(invoice.getAmount())
                            .taxAmount(invoice.getTaxAmount())
                            .invoiceDate(invoice.getInvoiceDate())
                            .linkAmount(invoice.getTotalAmount() != null ? invoice.getTotalAmount() : invoice.getAmount())
                            .build();
                    invoiceLinkMapper.insert(link);
                    linkedInvoiceIds.add(invoice.getId());

                    updateItemInvoiceStatus(item.getId());
                    linked++;
                    break; // 一张发票只关联一个明细
                }
            }
        }

        log.info("Auto-linked {} invoices for bill {}", linked, billId);
        return linked;
    }
}
