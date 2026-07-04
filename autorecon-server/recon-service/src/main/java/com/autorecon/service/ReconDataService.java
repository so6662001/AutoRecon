package com.autorecon.service;

import com.autorecon.domain.dto.ReconBillItemDTO;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.vo.ExcelAnalysisVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 对账数据服务接口
 */
public interface ReconDataService {

    /**
     * 上传 Excel 导入买方数据
     */
    List<ReconBillItem> uploadExcel(Long billId, MultipartFile file);

    /**
     * 在线提交买方数据
     */
    void onlineSubmit(Long billId, List<ReconBillItemDTO> buyerItems);

    /**
     * 获取 Excel 列映射配置
     */
    Map<String, String> getExcelMapping(Long buyerId);

    /**
     * 保存 Excel 列映射配置
     */
    void saveExcelMapping(Long buyerId, Map<String, String> mapping);

    /**
     * 分析上传文件的表头并返回智能映射建议（含买方已保存映射）
     */
    ExcelAnalysisVO analyzeExcelHeaders(MultipartFile file, Long buyerId);

    /**
     * 使用指定或自动推断的映射解析 Excel 并导入买方数据
     */
    List<ReconBillItem> uploadExcelWithMapping(Long billId, MultipartFile file, Map<String, String> mapping,
                                                Boolean saveMapping, Long buyerId);
}
