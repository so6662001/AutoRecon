package com.autorecon.service.impl;

import com.autorecon.common.result.PageResult;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.entity.AuditLog;
import com.autorecon.domain.entity.SysUser;
import com.autorecon.mapper.AuditLogMapper;
import com.autorecon.mapper.SysUserMapper;
import com.autorecon.service.AuditLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 审计日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void log(String module, String action, String targetType, Long targetId, String detail) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        Long userId = SecurityUtil.getCurrentUserId();
        String userName = null;
        if (userId != null) {
            SysUser user = sysUserMapper.selectById(userId);
            if (user != null) userName = user.getRealName() != null ? user.getRealName() : user.getUsername();
        }
        String ip = "127.0.0.1";

        AuditLog auditLog = AuditLog.builder()
                .enterpriseId(enterpriseId)
                .userId(userId)
                .userName(userName)
                .module(module)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .detail(detail)
                .ipAddress(ip)
                .build();
        auditLogMapper.insert(auditLog);
    }

    @Override
    public PageResult<AuditLog> queryLogs(Long enterpriseId, String module, Integer pageNum, Integer pageSize) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getEnterpriseId, eid);
        if (StringUtils.isNotBlank(module)) {
            wrapper.eq(AuditLog::getModule, module);
        }
        wrapper.orderByDesc(AuditLog::getCreatedAt);

        int pn = pageNum != null && pageNum > 0 ? pageNum : 1;
        int ps = pageSize != null && pageSize > 0 ? pageSize : 10;
        Page<AuditLog> page = new Page<>(pn, ps);
        IPage<AuditLog> result = auditLogMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    @Override
    public List<AuditLog> listByTarget(String targetType, Long targetId) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getTargetType, targetType)
                .eq(AuditLog::getTargetId, targetId)
                .orderByDesc(AuditLog::getCreatedAt);
        return auditLogMapper.selectList(wrapper);
    }
}
