package com.pickupexpress.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pickupexpress.domain.dto.SupplementCreateDTO;
import com.pickupexpress.domain.entity.SupplementRecord;

import java.util.List;

/**
 * 事后补录服务
 */
public interface SupplementService extends IService<SupplementRecord> {

    Long createSupplement(SupplementCreateDTO dto);

    void approve(Long id, String comment);

    void reject(Long id, String comment);

    List<SupplementRecord> listPending(Long enterpriseId);

    void appendDocumentUrl(Long id, String fileUrl);
}
