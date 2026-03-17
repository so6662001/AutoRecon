package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.SealOperatorCreateDTO;
import com.autorecon.domain.dto.SealOperatorUpdateDTO;
import com.autorecon.domain.entity.SealOperator;
import com.autorecon.mapper.SealOperatorMapper;
import com.autorecon.service.SealOperatorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 签章经办人服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SealOperatorServiceImpl extends ServiceImpl<SealOperatorMapper, SealOperator> implements SealOperatorService {

    private final SealOperatorMapper sealOperatorMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addOperator(SealOperatorCreateDTO dto) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = SecurityUtil.getCurrentUserId();

        SealOperator operator = SealOperator.builder()
                .enterpriseId(enterpriseId)
                .userId(userId)
                .operatorName(dto.getOperatorName())
                .phone(dto.getPhone())
                .idNo(dto.getIdNo())
                .allowedSealTypes(dto.getAllowedSealTypes())
                .amountLimit(dto.getAmountLimit())
                .requireApproval(dto.getRequireApproval())
                .verifyMethod(dto.getVerifyMethod())
                .status(1)
                .build();

        sealOperatorMapper.insert(operator);
        log.info("Added seal operator: id={}, enterpriseId={}", operator.getId(), enterpriseId);
        return operator.getId();
    }

    @Override
    public List<SealOperator> listOperators(Long enterpriseId) {
        LambdaQueryWrapper<SealOperator> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SealOperator::getEnterpriseId, enterpriseId)
                .orderByDesc(SealOperator::getCreatedAt);
        return sealOperatorMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOperator(Long id, SealOperatorUpdateDTO dto) {
        SealOperator operator = sealOperatorMapper.selectById(id);
        if (operator == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "经办人不存在");
        }

        if (StringUtils.hasText(dto.getOperatorName())) {
            operator.setOperatorName(dto.getOperatorName());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            operator.setPhone(dto.getPhone());
        }
        if (dto.getIdNo() != null) {
            operator.setIdNo(dto.getIdNo());
        }
        if (dto.getAllowedSealTypes() != null) {
            operator.setAllowedSealTypes(dto.getAllowedSealTypes());
        }
        if (dto.getAmountLimit() != null) {
            operator.setAmountLimit(dto.getAmountLimit());
        }
        if (dto.getRequireApproval() != null) {
            operator.setRequireApproval(dto.getRequireApproval());
        }
        if (dto.getVerifyMethod() != null) {
            operator.setVerifyMethod(dto.getVerifyMethod());
        }

        sealOperatorMapper.updateById(operator);
        log.info("Updated seal operator: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableOperator(Long id) {
        SealOperator operator = sealOperatorMapper.selectById(id);
        if (operator == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "经办人不存在");
        }
        operator.setStatus(2);
        sealOperatorMapper.updateById(operator);
        log.info("Disabled seal operator: id={}", id);
    }
}
