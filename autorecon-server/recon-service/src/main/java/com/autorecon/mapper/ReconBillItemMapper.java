package com.autorecon.mapper;

import com.autorecon.domain.entity.ReconBillItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 对账单明细 Mapper
 */
@Mapper
public interface ReconBillItemMapper extends BaseMapper<ReconBillItem> {

    /**
     * 根据对账单ID查询明细列表
     */
    List<ReconBillItem> selectByBillId(@Param("billId") Long billId);
}
