package com.autorecon.mapper;

import com.autorecon.domain.dto.ReconBillQueryDTO;
import com.autorecon.domain.entity.ReconBill;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 对账单 Mapper
 */
@Mapper
public interface ReconBillMapper extends BaseMapper<ReconBill> {

    /**
     * 分页查询对账单列表
     */
    List<ReconBill> selectBillList(@Param("query") ReconBillQueryDTO query);
}
