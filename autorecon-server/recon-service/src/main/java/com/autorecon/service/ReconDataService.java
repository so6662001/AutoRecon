package com.autorecon.service;

import com.autorecon.domain.dto.ReconBillItemDTO;
import com.autorecon.domain.entity.ReconBillItem;
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
}
