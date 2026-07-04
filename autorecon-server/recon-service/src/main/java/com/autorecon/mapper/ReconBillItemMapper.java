package com.autorecon.mapper;

import com.autorecon.domain.entity.ReconBillItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 对账单明细 Mapper
 */
@Mapper
public interface ReconBillItemMapper extends BaseMapper<ReconBillItem> {

    /**
     * 根据对账单ID查询明细列表
     */
    @Select("SELECT * FROM recon_bill_item WHERE bill_id = #{billId} AND deleted = 0 ORDER BY line_no")
    List<ReconBillItem> selectByBillId(@Param("billId") Long billId);
}
