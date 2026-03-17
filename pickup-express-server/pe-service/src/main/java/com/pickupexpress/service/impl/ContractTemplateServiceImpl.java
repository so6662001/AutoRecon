package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.common.exception.ErrorCode;
import com.pickupexpress.common.util.SecurityUtil;
import com.pickupexpress.common.util.TenantUtil;
import com.pickupexpress.domain.dto.ContractTemplateCreateDTO;
import com.pickupexpress.domain.entity.ContractTemplate;
import com.pickupexpress.mapper.ContractTemplateMapper;
import com.pickupexpress.service.ContractTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 合同模板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractTemplateServiceImpl extends ServiceImpl<ContractTemplateMapper, ContractTemplate> implements ContractTemplateService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(ContractTemplateCreateDTO dto) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        ContractTemplate template = new ContractTemplate();
        BeanUtils.copyProperties(dto, template);
        template.setEnterpriseId(enterpriseId);
        template.setVersion(1);
        template.setStatus(1);
        save(template);
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(Long id, ContractTemplateCreateDTO dto) {
        ContractTemplate template = getById(id);
        if (template == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        TenantUtil.checkOwnership(template.getEnterpriseId());
        BeanUtils.copyProperties(dto, template, "id", "enterpriseId", "createdAt");
        template.setVersion(template.getVersion() != null ? template.getVersion() + 1 : 1);
        updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        ContractTemplate template = getById(id);
        if (template == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        TenantUtil.checkOwnership(template.getEnterpriseId());
        removeById(id);
    }

    @Override
    public List<ContractTemplate> listTemplates(Long enterpriseId, Integer contractType) {
        LambdaQueryWrapper<ContractTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractTemplate::getEnterpriseId, enterpriseId);
        wrapper.eq(contractType != null, ContractTemplate::getContractType, contractType);
        wrapper.orderByDesc(ContractTemplate::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public ContractTemplate getDefaultTemplate(Long enterpriseId, Integer contractType) {
        return getOne(new LambdaQueryWrapper<ContractTemplate>()
                .eq(ContractTemplate::getEnterpriseId, enterpriseId)
                .eq(ContractTemplate::getContractType, contractType)
                .eq(ContractTemplate::getIsDefault, 1)
                .last("LIMIT 1"));
    }
}
