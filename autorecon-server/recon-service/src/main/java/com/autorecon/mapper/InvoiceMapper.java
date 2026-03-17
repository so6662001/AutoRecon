package com.autorecon.mapper;

import com.autorecon.domain.entity.Invoice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发票记录 Mapper
 */
@Mapper
public interface InvoiceMapper extends BaseMapper<Invoice> {
}
