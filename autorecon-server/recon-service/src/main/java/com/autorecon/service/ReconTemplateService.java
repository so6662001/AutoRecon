package com.autorecon.service;

import com.autorecon.domain.dto.TemplateCreateDTO;
import com.autorecon.domain.entity.ReconTemplate;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 对账单模板服务接口
 */
public interface ReconTemplateService extends IService<ReconTemplate> {

    Long createTemplate(TemplateCreateDTO dto);

    void updateTemplate(Long id, TemplateCreateDTO dto);

    void deleteTemplate(Long id);

    List<ReconTemplate> listTemplates(Long enterpriseId);

    ReconTemplate getDefaultTemplate(Long enterpriseId);

    Long copyTemplate(Long id);
}
