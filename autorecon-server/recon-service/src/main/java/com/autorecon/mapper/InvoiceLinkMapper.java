package com.autorecon.mapper;

import com.autorecon.domain.entity.InvoiceLink;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发票关联 Mapper
 */
@Mapper
public interface InvoiceLinkMapper extends BaseMapper<InvoiceLink> {
}
