package com.autorecon.service;

import com.autorecon.common.result.PageResult;
import com.autorecon.domain.dto.InvoiceCreateDTO;
import com.autorecon.domain.dto.InvoiceLinkDTO;
import com.autorecon.domain.entity.Invoice;
import com.autorecon.domain.entity.InvoiceLink;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 发票服务接口
 */
public interface InvoiceService extends IService<Invoice> {

    Long createInvoice(InvoiceCreateDTO dto);

    PageResult<Invoice> listInvoices(Long enterpriseId, Integer pageNum, Integer pageSize);

    void linkInvoiceToBillItem(InvoiceLinkDTO dto);

    List<InvoiceLink> getInvoiceLinks(Long billId);

    void unlinkInvoice(Long linkId);
}
