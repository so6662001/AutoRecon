package com.autorecon.service;

import com.autorecon.domain.dto.AuthorizationItemDTO;
import com.autorecon.domain.vo.AuthorizationCheckResult;
import com.autorecon.domain.vo.EnterpriseAuthorizationVO;

import java.util.List;

/**
 * 企业数据授权（两级模型中的企业级）
 */
public interface DataAuthorizationService {

    void initializeForEnterprise(Long enterpriseId, Long adminUserId, String ip, String userAgent);

    EnterpriseAuthorizationVO getAuthorizationStatus(Long enterpriseId);

    void updateAuthorizations(Long enterpriseId, List<AuthorizationItemDTO> items, String ip, String userAgent);

    void revokeAuthorization(Long enterpriseId, String authorizationType);

    boolean isAuthorized(Long enterpriseId, String authorizationType);

    boolean needsInitialAuthorization(Long enterpriseId);

    void notifyAuthorizationChange(String changeType, String summary, String newTypes);

    void respondToChange(Long notificationId, boolean accept, String detail);

    AuthorizationCheckResult checkAuthorization(Long enterpriseId);
}
