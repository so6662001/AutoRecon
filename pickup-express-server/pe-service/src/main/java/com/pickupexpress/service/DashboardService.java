package com.pickupexpress.service;

import com.pickupexpress.domain.vo.DashboardVO;

/**
 * 仪表盘服务
 */
public interface DashboardService {

    DashboardVO getDashboard(Long enterpriseId);
}
