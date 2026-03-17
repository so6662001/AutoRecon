package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.TenantUtil;
import com.autorecon.domain.entity.ToleranceLearn;
import com.autorecon.mapper.ToleranceLearnMapper;
import com.autorecon.service.ToleranceLearnService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 容差学习服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ToleranceLearnServiceImpl extends ServiceImpl<ToleranceLearnMapper, ToleranceLearn> implements ToleranceLearnService {

    private final ToleranceLearnMapper toleranceLearnMapper;

    @Override
    public List<ToleranceLearn> getSuggestions(Long sellerId, Long buyerId) {
        LambdaQueryWrapper<ToleranceLearn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ToleranceLearn::getSellerId, sellerId)
                .eq(ToleranceLearn::getBuyerId, buyerId)
                .eq(ToleranceLearn::getAdopted, 0)
                .orderByDesc(ToleranceLearn::getConfidence);
        return toleranceLearnMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adoptSuggestion(Long id) {
        ToleranceLearn learn = toleranceLearnMapper.selectById(id);
        if (learn == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "容差学习记录不存在");
        }
        TenantUtil.checkOwnership(learn.getSellerId());
        learn.setAdopted(1);
        toleranceLearnMapper.updateById(learn);
        log.info("Adopted tolerance suggestion: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectSuggestion(Long id) {
        ToleranceLearn learn = toleranceLearnMapper.selectById(id);
        if (learn == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "容差学习记录不存在");
        }
        TenantUtil.checkOwnership(learn.getSellerId());
        toleranceLearnMapper.deleteById(id);
        log.info("Rejected tolerance suggestion: id={}", id);
    }

    @Override
    public List<ToleranceLearn> listAll(Long sellerId) {
        LambdaQueryWrapper<ToleranceLearn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ToleranceLearn::getSellerId, sellerId)
                .orderByDesc(ToleranceLearn::getCalculatedAt);
        return toleranceLearnMapper.selectList(wrapper);
    }
}
