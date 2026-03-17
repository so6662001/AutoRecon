package com.pickupexpress.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pickupexpress.domain.dto.ContractTemplateCreateDTO;
import com.pickupexpress.domain.entity.ContractTemplate;

import java.util.List;

/**
 * 合同模板服务接口
 */
public interface ContractTemplateService extends IService<ContractTemplate> {

    Long createTemplate(ContractTemplateCreateDTO dto);

    void updateTemplate(Long id, ContractTemplateCreateDTO dto);

    void deleteTemplate(Long id);

    List<ContractTemplate> listTemplates(Long enterpriseId, Integer contractType);

    ContractTemplate getDefaultTemplate(Long enterpriseId, Integer contractType);
}
