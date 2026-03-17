package com.autorecon.service;

import com.autorecon.domain.dto.ReconBillCreateDTO;
import com.autorecon.domain.dto.ReconBillQueryDTO;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.vo.DashboardVO;
import com.autorecon.domain.vo.ReconBillDetailVO;
import com.autorecon.domain.vo.ReconBillVO;
import com.autorecon.common.result.PageResult;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

/**
 * 对账单服务接口
 */
public interface ReconBillService extends IService<ReconBill> {

    /**
     * 创建对账单（含明细）
     */
    Long createBill(ReconBillCreateDTO dto);

    /**
     * 分页查询对账单列表
     */
    PageResult<ReconBillVO> queryBillList(ReconBillQueryDTO query);

    /**
     * 获取对账单详情
     */
    ReconBillDetailVO getBillDetail(Long billId);

    /**
     * 发送对账单给买方
     */
    void sendBill(Long billId);

    /**
     * 买方确认（无异议）
     */
    void confirmBill(Long billId);

    /**
     * 作废对账单
     */
    void voidBill(Long billId);

    /**
     * 获取仪表盘统计
     */
    DashboardVO getDashboard(Long enterpriseId);

    /**
     * 生成对账单PDF（异步占位）
     */
    String generatePdf(Long billId);

    /**
     * 批量创建对账单
     */
    String batchCreateBills(List<Long> buyerIds, LocalDate periodStart, LocalDate periodEnd, Long templateId);
}
