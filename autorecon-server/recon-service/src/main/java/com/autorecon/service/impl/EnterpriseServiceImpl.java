package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.TenantUtil;
import com.autorecon.common.result.PageResult;
import com.autorecon.domain.dto.EnterpriseAuthDTO;
import com.autorecon.domain.dto.EnterpriseCreateDTO;
import com.autorecon.domain.entity.Enterprise;
import com.autorecon.domain.entity.EnterpriseAuth;
import com.autorecon.mapper.EnterpriseAuthMapper;
import com.autorecon.mapper.EnterpriseMapper;
import com.autorecon.service.EnterpriseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 企业服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseServiceImpl extends ServiceImpl<EnterpriseMapper, Enterprise> implements EnterpriseService {

    private final EnterpriseMapper enterpriseMapper;
    private final EnterpriseAuthMapper enterpriseAuthMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEnterprise(EnterpriseCreateDTO dto) {
        Enterprise enterprise = Enterprise.builder()
                .companyName(dto.getCompanyName())
                .unifiedCreditCode(dto.getUnifiedCreditCode())
                .contactName(dto.getContactName())
                .contactPhone(dto.getContactPhone())
                .contactEmail(dto.getContactEmail())
                .enterpriseType(dto.getEnterpriseType())
                .logoUrl(dto.getLogoUrl())
                .status(1)
                .build();
        enterpriseMapper.insert(enterprise);
        log.info("Created enterprise: id={}", enterprise.getId());
        return enterprise.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnterprise(Long id, EnterpriseCreateDTO dto) {
        Enterprise enterprise = enterpriseMapper.selectById(id);
        if (enterprise == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "企业不存在");
        }
        TenantUtil.checkOwnership(id);
        enterprise.setCompanyName(dto.getCompanyName());
        enterprise.setUnifiedCreditCode(dto.getUnifiedCreditCode());
        enterprise.setContactName(dto.getContactName());
        enterprise.setContactPhone(dto.getContactPhone());
        enterprise.setContactEmail(dto.getContactEmail());
        enterprise.setEnterpriseType(dto.getEnterpriseType());
        enterprise.setLogoUrl(dto.getLogoUrl());
        enterpriseMapper.updateById(enterprise);
        log.info("Updated enterprise: id={}", id);
    }

    @Override
    public Enterprise getEnterprise(Long id) {
        Enterprise enterprise = enterpriseMapper.selectById(id);
        if (enterprise == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "企业不存在");
        }
        return enterprise;
    }

    @Override
    public PageResult<Enterprise> listEnterprises(String keyword, Integer type, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Enterprise> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Enterprise::getCompanyName, keyword)
                    .or().like(Enterprise::getUnifiedCreditCode, keyword)
                    .or().like(Enterprise::getContactName, keyword));
        }
        if (type != null) {
            wrapper.eq(Enterprise::getEnterpriseType, type);
        }
        wrapper.orderByDesc(Enterprise::getCreatedAt);

        int pn = pageNum != null && pageNum > 0 ? pageNum : 1;
        int ps = pageSize != null && pageSize > 0 ? pageSize : 10;
        Page<Enterprise> page = new Page<>(pn, ps);
        IPage<Enterprise> result = enterpriseMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAuth(Long enterpriseId, EnterpriseAuthDTO dto) {
        TenantUtil.checkOwnership(enterpriseId);
        LambdaQueryWrapper<EnterpriseAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnterpriseAuth::getEnterpriseId, enterpriseId).last("LIMIT 1");
        EnterpriseAuth auth = enterpriseAuthMapper.selectOne(wrapper);
        if (auth == null) {
            auth = EnterpriseAuth.builder()
                    .enterpriseId(enterpriseId)
                    .companyName(dto.getCompanyName())
                    .unifiedCreditCode(dto.getUnifiedCreditCode())
                    .legalPersonName(dto.getLegalPersonName())
                    .legalPersonIdNo(dto.getLegalPersonIdNo())
                    .legalPersonPhone(dto.getLegalPersonPhone())
                    .businessLicenseUrl(dto.getBusinessLicenseUrl())
                    .legalPersonIdFrontUrl(dto.getLegalPersonIdFrontUrl())
                    .legalPersonIdBackUrl(dto.getLegalPersonIdBackUrl())
                    .authorizationLetterUrl(dto.getAuthorizationLetterUrl())
                    .authStatus(1)
                    .build();
            enterpriseAuthMapper.insert(auth);
        } else {
            auth.setCompanyName(dto.getCompanyName());
            auth.setUnifiedCreditCode(dto.getUnifiedCreditCode());
            auth.setLegalPersonName(dto.getLegalPersonName());
            auth.setLegalPersonIdNo(dto.getLegalPersonIdNo());
            auth.setLegalPersonPhone(dto.getLegalPersonPhone());
            auth.setBusinessLicenseUrl(dto.getBusinessLicenseUrl());
            auth.setLegalPersonIdFrontUrl(dto.getLegalPersonIdFrontUrl());
            auth.setLegalPersonIdBackUrl(dto.getLegalPersonIdBackUrl());
            auth.setAuthorizationLetterUrl(dto.getAuthorizationLetterUrl());
            auth.setAuthStatus(1);
            enterpriseAuthMapper.updateById(auth);
        }
        log.info("Submitted enterprise auth: enterpriseId={}", enterpriseId);
    }

    @Override
    public EnterpriseAuth getAuthStatus(Long enterpriseId) {
        LambdaQueryWrapper<EnterpriseAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnterpriseAuth::getEnterpriseId, enterpriseId).last("LIMIT 1");
        return enterpriseAuthMapper.selectOne(wrapper);
    }
}
