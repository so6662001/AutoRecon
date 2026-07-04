package com.pickupexpress.service;

import com.pickupexpress.domain.dto.AnalyticsCollectDTO;
import com.pickupexpress.domain.vo.FunnelDataVO;
import com.pickupexpress.domain.vo.PageValueVO;
import com.pickupexpress.domain.vo.RealtimeOverviewVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 分析服务接口
 */
public interface AnalyticsService {

    void collectEvents(AnalyticsCollectDTO dto, String ip);

    RealtimeOverviewVO getRealtimeOverview(String system);

    List<PageValueVO> getPageRanking(String system, LocalDate start, LocalDate end, String module);

    List<Map<String, Object>> getHotPages(String system, int topN);

    List<Map<String, Object>> getActionStats(String system, LocalDate start, LocalDate end);

    FunnelDataVO getFunnelData(Long funnelId, LocalDate start, LocalDate end);

    Map<String, Object> getRetentionData(String system, LocalDate start, int days);

    Map<String, Object> getDeviceDistribution(String system, LocalDate start, LocalDate end);

    Map<String, Object> getPerformanceOverview(String system, LocalDate start, LocalDate end);

    List<Map<String, Object>> getFeatureAdoption(String system, LocalDate start, LocalDate end);
}
