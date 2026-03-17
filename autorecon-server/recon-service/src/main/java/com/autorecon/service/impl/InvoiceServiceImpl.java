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
import com.autorecon.mapper.InvoiceLinkMapper;
import com.autorecon.mapper.InvoiceMapper;
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
        log.info("Linked invoice to bill: billId={}, invoiceId={}", dto.getBillId(), dto.getInvoiceId());
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
        invoiceLinkMapper.deleteById(linkId);
        log.info("Unlinked invoice: linkId={}", linkId);
    }
}
