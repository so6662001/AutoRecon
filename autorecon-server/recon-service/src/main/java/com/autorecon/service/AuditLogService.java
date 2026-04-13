package com.autorecon.service;

import com.autorecon.common.result.PageResult;
import com.autorecon.domain.entity.AuditLog;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 审计日志服务接口
 */
public interface AuditLogService extends IService<AuditLog> {

    void log(String module, String action, String targetType, Long targetId, String detail);

    /**
     * 审计日志（含 IP 与 User-Agent，用于数据授权等场景）
     */
    void log(String module, String action, String targetType, Long targetId, String detail,
             String ipAddress, String userAgent);

    PageResult<AuditLog> queryLogs(Long enterpriseId, String module, Integer pageNum, Integer pageSize);

    /**
     * 按目标类型与目标 ID 查询审计记录（如自动对账计划执行日志）
     */
    List<AuditLog> listByTarget(String targetType, Long targetId);
}
