package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.TemplateCreateDTO;
import com.autorecon.domain.entity.ReconTemplate;
import com.autorecon.mapper.ReconTemplateMapper;
import com.autorecon.service.ReconTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 对账单模板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconTemplateServiceImpl extends ServiceImpl<ReconTemplateMapper, ReconTemplate> implements ReconTemplateService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(TemplateCreateDTO dto) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            enterpriseId = 1L;
        }

        ReconTemplate template = new ReconTemplate();
        BeanUtils.copyProperties(dto, template);
        template.setEnterpriseId(enterpriseId);
        template.setStatus(1);
        template.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : 0);

        if (template.getIsDefault() == 1) {
            LambdaUpdateWrapper<ReconTemplate> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ReconTemplate::getEnterpriseId, enterpriseId).set(ReconTemplate::getIsDefault, 0);
            baseMapper.update(null, updateWrapper);
        }

        baseMapper.insert(template);
        log.info("Created template: id={}, name={}", template.getId(), template.getTemplateName());
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(Long id, TemplateCreateDTO dto) {
        ReconTemplate template = baseMapper.selectById(id);
        if (template == null) {
            throw new BizException(ErrorCode.TEMPLATE_NOT_FOUND);
        }
        BeanUtils.copyProperties(dto, template, "id", "enterpriseId", "createdAt");
        template.setId(id);

        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            LambdaUpdateWrapper<ReconTemplate> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ReconTemplate::getEnterpriseId, template.getEnterpriseId()).set(ReconTemplate::getIsDefault, 0);
            baseMapper.update(null, updateWrapper);
        }

        baseMapper.updateById(template);
        log.info("Updated template: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        ReconTemplate template = baseMapper.selectById(id);
        if (template == null) {
            throw new BizException(ErrorCode.TEMPLATE_NOT_FOUND);
        }
        baseMapper.deleteById(id);
        log.info("Deleted template: id={}", id);
    }

    @Override
    public List<ReconTemplate> listTemplates(Long enterpriseId) {
        LambdaQueryWrapper<ReconTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconTemplate::getEnterpriseId, enterpriseId)
                .eq(ReconTemplate::getStatus, 1)
                .orderByDesc(ReconTemplate::getIsDefault)
                .orderByDesc(ReconTemplate::getCreatedAt);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public ReconTemplate getDefaultTemplate(Long enterpriseId) {
        LambdaQueryWrapper<ReconTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconTemplate::getEnterpriseId, enterpriseId)
                .eq(ReconTemplate::getIsDefault, 1)
                .eq(ReconTemplate::getStatus, 1)
                .last("LIMIT 1");
        return baseMapper.selectOne(wrapper);
    }
}
