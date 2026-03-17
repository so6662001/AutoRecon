package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.SealCreateDTO;
import com.autorecon.domain.entity.EnterpriseSeal;
import com.autorecon.mapper.EnterpriseSealMapper;
import com.autorecon.service.EnterpriseSealService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 企业印章服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseSealServiceImpl extends ServiceImpl<EnterpriseSealMapper, EnterpriseSeal> implements EnterpriseSealService {

    private final EnterpriseSealMapper enterpriseSealMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSeal(SealCreateDTO dto) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        EnterpriseSeal seal = EnterpriseSeal.builder()
                .enterpriseId(enterpriseId)
                .sealName(dto.getSealName())
                .sealType(dto.getSealType())
                .sealSource(dto.getSealSource())
                .sealImageUrl(dto.getSealImageUrl())
                .status(1)
                .legalPersonConfirmed(0)
                .build();

        enterpriseSealMapper.insert(seal);
        log.info("Created seal: id={}, enterpriseId={}", seal.getId(), enterpriseId);
        return seal.getId();
    }

    @Override
    public List<EnterpriseSeal> listSeals(Long enterpriseId) {
        LambdaQueryWrapper<EnterpriseSeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnterpriseSeal::getEnterpriseId, enterpriseId)
                .orderByDesc(EnterpriseSeal::getCreatedAt);
        return enterpriseSealMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableSeal(Long sealId) {
        EnterpriseSeal seal = enterpriseSealMapper.selectById(sealId);
        if (seal == null) {
            throw new BizException(ErrorCode.SIGN_SEAL_NOT_FOUND);
        }
        seal.setStatus(2);
        seal.setDisabledAt(LocalDateTime.now());
        enterpriseSealMapper.updateById(seal);
        log.info("Disabled seal: sealId={}", sealId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableSeal(Long sealId) {
        EnterpriseSeal seal = enterpriseSealMapper.selectById(sealId);
        if (seal == null) {
            throw new BizException(ErrorCode.SIGN_SEAL_NOT_FOUND);
        }
        seal.setStatus(1);
        seal.setDisabledAt(null);
        enterpriseSealMapper.updateById(seal);
        log.info("Enabled seal: sealId={}", sealId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeSeal(Long sealId) {
        EnterpriseSeal seal = enterpriseSealMapper.selectById(sealId);
        if (seal == null) {
            throw new BizException(ErrorCode.SIGN_SEAL_NOT_FOUND);
        }
        seal.setStatus(3);
        enterpriseSealMapper.updateById(seal);
        log.info("Revoked seal: sealId={}", sealId);
    }
}
