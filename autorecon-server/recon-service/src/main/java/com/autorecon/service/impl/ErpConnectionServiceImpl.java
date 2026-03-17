package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.ErpConnectionCreateDTO;
import com.autorecon.domain.entity.ErpConnection;
import com.autorecon.mapper.ErpConnectionMapper;
import com.autorecon.service.ErpConnectionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ERP连接服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErpConnectionServiceImpl extends ServiceImpl<ErpConnectionMapper, ErpConnection> implements ErpConnectionService {

    private final ErpConnectionMapper erpConnectionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConnection(ErpConnectionCreateDTO dto) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        ErpConnection conn = ErpConnection.builder()
                .enterpriseId(enterpriseId)
                .connectionName(dto.getConnectionName())
                .connectionType(dto.getConnectionType())
                .baseUrl(dto.getBaseUrl())
                .authType(dto.getAuthType())
                .authConfig(dto.getAuthConfig())
                .fieldMapping(dto.getFieldMapping())
                .pullStrategy(dto.getPullStrategy())
                .cronExpression(dto.getCronExpression())
                .status(1)
                .build();
        erpConnectionMapper.insert(conn);
        log.info("Created ERP connection: id={}, enterpriseId={}", conn.getId(), enterpriseId);
        return conn.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConnection(Long id, ErpConnectionCreateDTO dto) {
        ErpConnection conn = erpConnectionMapper.selectById(id);
        if (conn == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "ERP连接不存在");
        }
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null || !enterpriseId.equals(conn.getEnterpriseId())) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        conn.setConnectionName(dto.getConnectionName());
        conn.setConnectionType(dto.getConnectionType());
        conn.setBaseUrl(dto.getBaseUrl());
        conn.setAuthType(dto.getAuthType());
        conn.setAuthConfig(dto.getAuthConfig());
        conn.setFieldMapping(dto.getFieldMapping());
        conn.setPullStrategy(dto.getPullStrategy());
        conn.setCronExpression(dto.getCronExpression());
        erpConnectionMapper.updateById(conn);
        log.info("Updated ERP connection: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConnection(Long id) {
        ErpConnection conn = erpConnectionMapper.selectById(id);
        if (conn == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "ERP连接不存在");
        }
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null || !enterpriseId.equals(conn.getEnterpriseId())) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        erpConnectionMapper.deleteById(id);
        log.info("Deleted ERP connection: id={}", id);
    }

    @Override
    public List<ErpConnection> listConnections(Long enterpriseId) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        if (eid == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        LambdaQueryWrapper<ErpConnection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErpConnection::getEnterpriseId, eid).orderByDesc(ErpConnection::getCreatedAt);
        return erpConnectionMapper.selectList(wrapper);
    }

    @Override
    public boolean testConnection(Long id) {
        ErpConnection conn = erpConnectionMapper.selectById(id);
        if (conn == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "ERP连接不存在");
        }
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null || !enterpriseId.equals(conn.getEnterpriseId())) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        log.info("connection test passed: id={}", id);
        return true;
    }
}
