package com.autorecon.service;

import com.autorecon.common.result.PageResult;
import com.autorecon.domain.entity.AuditLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 审计日志服务接口
 */
public interface AuditLogService extends IService<AuditLog> {

    void log(String module, String action, String targetType, Long targetId, String detail);

    PageResult<AuditLog> queryLogs(Long enterpriseId, String module, Integer pageNum, Integer pageSize);
}
