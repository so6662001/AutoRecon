package com.pickupexpress.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pickupexpress.domain.entity.EvidencePackage;
import com.pickupexpress.domain.vo.EvidencePackageVO;

/**
 * 证据包服务接口
 */
public interface EvidenceService extends IService<EvidencePackage> {

    Long archiveEvidence(Long pickupOrderId);

    EvidencePackageVO getEvidencePackage(Long pickupOrderId);

    boolean verifyIntegrity(Long evidencePackageId);

    boolean verifyIntegrityByPickupOrderId(Long pickupOrderId);
}
