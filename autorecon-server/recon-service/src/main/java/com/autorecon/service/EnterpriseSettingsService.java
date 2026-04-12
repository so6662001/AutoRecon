package com.autorecon.service;

import com.autorecon.domain.dto.NotificationTemplateDTO;
import com.autorecon.domain.dto.ReconRulesDTO;
import com.autorecon.domain.dto.TimeoutConfigDTO;

public interface EnterpriseSettingsService {

    TimeoutConfigDTO getTimeoutConfig(Long enterpriseId);

    void saveTimeoutConfig(Long enterpriseId, TimeoutConfigDTO dto);

    NotificationTemplateDTO getNotificationTemplates(Long enterpriseId);

    void saveNotificationTemplates(Long enterpriseId, NotificationTemplateDTO dto);

    ReconRulesDTO getReconRules(Long enterpriseId);

    void saveReconRules(Long enterpriseId, ReconRulesDTO dto);
}
