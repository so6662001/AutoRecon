package com.autorecon.service.impl;

import com.autorecon.domain.dto.AnalyticsCollectDTO;
import com.autorecon.domain.entity.AnalyticsActionDaily;
import com.autorecon.domain.entity.AnalyticsEvent;
import com.autorecon.domain.entity.AnalyticsFunnelConfig;
import com.autorecon.domain.entity.AnalyticsPageDaily;
import com.autorecon.domain.vo.FunnelDataVO;
import com.autorecon.domain.vo.PageValueVO;
import com.autorecon.domain.vo.RealtimeOverviewVO;
import com.autorecon.mapper.AnalyticsActionDailyMapper;
import com.autorecon.mapper.AnalyticsEventMapper;
import com.autorecon.mapper.AnalyticsFunnelConfigMapper;
import com.autorecon.mapper.AnalyticsPageDailyMapper;
import com.autorecon.service.AnalyticsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsEventMapper eventMapper;
    private final AnalyticsPageDailyMapper pageDailyMapper;
    private final AnalyticsActionDailyMapper actionDailyMapper;
    private final AnalyticsFunnelConfigMapper funnelConfigMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectEvents(AnalyticsCollectDTO dto, String ip) {
        if (dto == null || CollectionUtils.isEmpty(dto.getEvents())) {
            return;
        }

        List<AnalyticsEvent> entities = new ArrayList<>();
        for (AnalyticsCollectDTO.EventDTO event : dto.getEvents()) {
            AnalyticsEvent entity = convertToEntity(event, ip);
            entities.add(entity);
        }

        for (AnalyticsEvent entity : entities) {
            try {
                eventMapper.insert(entity);
            } catch (Exception e) {
                log.warn("Duplicate event ignored: {}", entity.getEventId());
            }
        }

        updateDailyAggregates(entities);
    }

    @Override
    public RealtimeOverviewVO getRealtimeOverview(String system) {
        RealtimeOverviewVO vo = new RealtimeOverviewVO();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<AnalyticsEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(system), AnalyticsEvent::getSystem, system)
                .ge(AnalyticsEvent::getEventTime, todayStart)
                .le(AnalyticsEvent::getEventTime, now);

        List<AnalyticsEvent> todayEvents = eventMapper.selectList(wrapper);

        vo.setTodayEvents(todayEvents.size());

        long pvCount = todayEvents.stream()
                .filter(e -> "page_view".equals(e.getEventType()))
                .count();
        vo.setTodayPv((int) pvCount);

        long uvCount = todayEvents.stream()
                .filter(e -> "page_view".equals(e.getEventType()))
                .map(AnalyticsEvent::getUserId)
                .filter(Objects::nonNull)
                .filter(uid -> uid > 0)
                .distinct()
                .count();
        if (uvCount == 0) {
            uvCount = todayEvents.stream()
                    .filter(e -> "page_view".equals(e.getEventType()))
                    .map(AnalyticsEvent::getSessionId)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .count();
        }
        vo.setTodayUv((int) uvCount);

        LocalDateTime fiveMinAgo = now.minusMinutes(5);
        long onlineCount = todayEvents.stream()
                .filter(e -> e.getEventTime() != null && e.getEventTime().isAfter(fiveMinAgo))
                .map(AnalyticsEvent::getSessionId)
                .filter(StringUtils::hasText)
                .distinct()
                .count();
        vo.setCurrentOnline((int) onlineCount);

        OptionalDouble avgDur = todayEvents.stream()
                .filter(e -> e.getDuration() != null && e.getDuration() > 0)
                .mapToInt(AnalyticsEvent::getDuration)
                .average();
        vo.setAvgDuration(avgDur.isPresent() ? (int) avgDur.getAsDouble() : 0);

        return vo;
    }

    @Override
    public List<PageValueVO> getPageRanking(String system, LocalDate start, LocalDate end, String module) {
        LambdaQueryWrapper<AnalyticsPageDaily> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(system), AnalyticsPageDaily::getSystem, system)
                .ge(start != null, AnalyticsPageDaily::getStatDate, start)
                .le(end != null, AnalyticsPageDaily::getStatDate, end)
                .eq(StringUtils.hasText(module), AnalyticsPageDaily::getPageModule, module);

        List<AnalyticsPageDaily> records = pageDailyMapper.selectList(wrapper);

        Map<String, List<AnalyticsPageDaily>> grouped = records.stream()
                .collect(Collectors.groupingBy(AnalyticsPageDaily::getPagePath));

        List<PageValueVO> result = new ArrayList<>();
        for (Map.Entry<String, List<AnalyticsPageDaily>> entry : grouped.entrySet()) {
            List<AnalyticsPageDaily> pages = entry.getValue();
            PageValueVO vo = new PageValueVO();
            vo.setPagePath(entry.getKey());
            vo.setPageName(pages.get(0).getPageName());
            vo.setPageModule(pages.get(0).getPageModule());

            int totalPv = pages.stream().mapToInt(p -> p.getPv() != null ? p.getPv() : 0).sum();
            int totalUv = pages.stream().mapToInt(p -> p.getUv() != null ? p.getUv() : 0).sum();
            int totalBounce = pages.stream().mapToInt(p -> p.getBounceCount() != null ? p.getBounceCount() : 0).sum();
            double avgDuration = pages.stream()
                    .filter(p -> p.getAvgDuration() != null)
                    .mapToInt(AnalyticsPageDaily::getAvgDuration)
                    .average().orElse(0);

            vo.setPv(totalPv);
            vo.setUv(totalUv);
            vo.setAvgDuration((int) avgDuration);
            vo.setBounceCount(totalBounce);
            vo.setBounceRate(totalPv > 0 ? Math.round((double) totalBounce / totalPv * 10000.0) / 100.0 : 0.0);
            vo.setConversionRate(totalPv > 0 ? Math.round((double) totalUv / totalPv * 10000.0) / 100.0 : 0.0);

            double valueScore = calculateValueScore(totalPv, totalUv, avgDuration, totalBounce, totalPv);
            vo.setValueScore(Math.round(valueScore * 100.0) / 100.0);
            vo.setValueLevel(assignValueLevel(valueScore));

            result.add(vo);
        }

        result.sort((a, b) -> Double.compare(b.getValueScore(), a.getValueScore()));
        return result;
    }

    @Override
    public List<Map<String, Object>> getHotPages(String system, int topN) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<AnalyticsPageDaily> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(system), AnalyticsPageDaily::getSystem, system)
                .eq(AnalyticsPageDaily::getStatDate, today)
                .orderByDesc(AnalyticsPageDaily::getPv)
                .last("LIMIT " + Math.min(topN, 100));

        List<AnalyticsPageDaily> records = pageDailyMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (AnalyticsPageDaily record : records) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("pagePath", record.getPagePath());
            map.put("pageName", record.getPageName());
            map.put("pv", record.getPv());
            map.put("uv", record.getUv());
            map.put("avgDuration", record.getAvgDuration());
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getActionStats(String system, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<AnalyticsActionDaily> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(system), AnalyticsActionDaily::getSystem, system)
                .ge(start != null, AnalyticsActionDaily::getStatDate, start)
                .le(end != null, AnalyticsActionDaily::getStatDate, end)
                .orderByDesc(AnalyticsActionDaily::getActionCount);

        List<AnalyticsActionDaily> records = actionDailyMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (AnalyticsActionDaily record : records) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("eventName", record.getEventName());
            map.put("actionCategory", record.getActionCategory());
            map.put("actionCount", record.getActionCount());
            map.put("actionUsers", record.getActionUsers());
            map.put("actionEnterprises", record.getActionEnterprises());
            map.put("statDate", record.getStatDate());
            result.add(map);
        }
        return result;
    }

    @Override
    public FunnelDataVO getFunnelData(Long funnelId, LocalDate start, LocalDate end) {
        AnalyticsFunnelConfig config = funnelConfigMapper.selectById(funnelId);
        if (config == null) {
            return null;
        }

        FunnelDataVO vo = new FunnelDataVO();
        vo.setFunnelName(config.getFunnelName());

        try {
            List<Map<String, String>> stepConfigs = objectMapper.readValue(
                    config.getStepsJson(), new TypeReference<>() {});

            LocalDateTime startTime = start != null ? start.atStartOfDay() : LocalDate.now().minusDays(7).atStartOfDay();
            LocalDateTime endTime = end != null ? end.atTime(LocalTime.MAX) : LocalDateTime.now();

            List<FunnelDataVO.FunnelStep> steps = new ArrayList<>();
            int firstCount = 0;

            for (int i = 0; i < stepConfigs.size(); i++) {
                Map<String, String> stepConfig = stepConfigs.get(i);
                String eventName = stepConfig.getOrDefault("eventName", "");
                String stepName = stepConfig.getOrDefault("name", eventName);

                LambdaQueryWrapper<AnalyticsEvent> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(AnalyticsEvent::getEventName, eventName)
                        .ge(AnalyticsEvent::getEventTime, startTime)
                        .le(AnalyticsEvent::getEventTime, endTime);

                if (StringUtils.hasText(config.getSystem())) {
                    wrapper.eq(AnalyticsEvent::getSystem, config.getSystem());
                }

                Long count = eventMapper.selectCount(wrapper);

                FunnelDataVO.FunnelStep step = new FunnelDataVO.FunnelStep();
                step.setName(stepName);
                step.setCount(count != null ? count.intValue() : 0);

                if (i == 0) {
                    firstCount = step.getCount();
                    step.setRate(100.0);
                } else {
                    step.setRate(firstCount > 0
                            ? Math.round((double) step.getCount() / firstCount * 10000.0) / 100.0
                            : 0.0);
                }

                steps.add(step);
            }

            vo.setSteps(steps);
        } catch (Exception e) {
            log.error("Failed to parse funnel config steps_json for funnelId={}", funnelId, e);
            vo.setSteps(Collections.emptyList());
        }

        return vo;
    }

    @Override
    public Map<String, Object> getRetentionData(String system, LocalDate start, int days) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", start);
        result.put("days", days);

        LocalDateTime startTime = start.atStartOfDay();

        LambdaQueryWrapper<AnalyticsEvent> day0Wrapper = new LambdaQueryWrapper<>();
        day0Wrapper.eq(StringUtils.hasText(system), AnalyticsEvent::getSystem, system)
                .ge(AnalyticsEvent::getEventTime, startTime)
                .lt(AnalyticsEvent::getEventTime, startTime.plusDays(1));

        List<AnalyticsEvent> day0Events = eventMapper.selectList(day0Wrapper);
        Set<Long> day0Users = day0Events.stream()
                .map(AnalyticsEvent::getUserId)
                .filter(Objects::nonNull)
                .filter(uid -> uid > 0)
                .collect(Collectors.toSet());

        result.put("day0Users", day0Users.size());

        List<Map<String, Object>> retention = new ArrayList<>();
        for (int d = 1; d <= days; d++) {
            LocalDateTime dayStart = startTime.plusDays(d);
            LocalDateTime dayEnd = dayStart.plusDays(1);

            LambdaQueryWrapper<AnalyticsEvent> dayWrapper = new LambdaQueryWrapper<>();
            dayWrapper.eq(StringUtils.hasText(system), AnalyticsEvent::getSystem, system)
                    .ge(AnalyticsEvent::getEventTime, dayStart)
                    .lt(AnalyticsEvent::getEventTime, dayEnd)
                    .in(!day0Users.isEmpty(), AnalyticsEvent::getUserId, day0Users);

            Set<Long> returnUsers;
            if (day0Users.isEmpty()) {
                returnUsers = Collections.emptySet();
            } else {
                List<AnalyticsEvent> dayEvents = eventMapper.selectList(dayWrapper);
                returnUsers = dayEvents.stream()
                        .map(AnalyticsEvent::getUserId)
                        .filter(day0Users::contains)
                        .collect(Collectors.toSet());
            }

            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("day", d);
            dayData.put("returnUsers", returnUsers.size());
            dayData.put("rate", day0Users.isEmpty() ? 0.0
                    : Math.round((double) returnUsers.size() / day0Users.size() * 10000.0) / 100.0);
            retention.add(dayData);
        }

        result.put("retention", retention);
        return result;
    }

    @Override
    public Map<String, Object> getDeviceDistribution(String system, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<AnalyticsEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(system), AnalyticsEvent::getSystem, system)
                .ge(start != null, AnalyticsEvent::getEventTime, start != null ? start.atStartOfDay() : null)
                .le(end != null, AnalyticsEvent::getEventTime, end != null ? end.atTime(LocalTime.MAX) : null)
                .eq(AnalyticsEvent::getEventType, "page_view");

        List<AnalyticsEvent> events = eventMapper.selectList(wrapper);

        Map<String, Long> platformDist = events.stream()
                .collect(Collectors.groupingBy(
                        e -> StringUtils.hasText(e.getPlatform()) ? e.getPlatform() : "unknown",
                        Collectors.counting()));

        Map<String, Long> osDist = events.stream()
                .collect(Collectors.groupingBy(
                        e -> StringUtils.hasText(e.getOs()) ? e.getOs() : "unknown",
                        Collectors.counting()));

        Map<String, Long> browserDist = events.stream()
                .collect(Collectors.groupingBy(
                        e -> StringUtils.hasText(e.getBrowser()) ? e.getBrowser() : "unknown",
                        Collectors.counting()));

        long mobileCount = events.stream().filter(e -> Integer.valueOf(1).equals(e.getIsMobile())).count();
        long desktopCount = events.size() - mobileCount;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("platform", platformDist);
        result.put("os", osDist);
        result.put("browser", browserDist);
        result.put("mobile", mobileCount);
        result.put("desktop", desktopCount);
        result.put("total", events.size());
        return result;
    }

    @Override
    public Map<String, Object> getPerformanceOverview(String system, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<AnalyticsEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(system), AnalyticsEvent::getSystem, system)
                .ge(start != null, AnalyticsEvent::getEventTime, start != null ? start.atStartOfDay() : null)
                .le(end != null, AnalyticsEvent::getEventTime, end != null ? end.atTime(LocalTime.MAX) : null)
                .eq(AnalyticsEvent::getEventType, "performance");

        List<AnalyticsEvent> events = eventMapper.selectList(wrapper);

        OptionalDouble avgFcp = events.stream()
                .filter(e -> e.getPerfFcp() != null && e.getPerfFcp() > 0)
                .mapToInt(e -> e.getPerfFcp().intValue())
                .average();

        OptionalDouble avgLcp = events.stream()
                .filter(e -> e.getPerfLcp() != null && e.getPerfLcp() > 0)
                .mapToInt(e -> e.getPerfLcp().intValue())
                .average();

        OptionalDouble avgFid = events.stream()
                .filter(e -> e.getPerfFid() != null && e.getPerfFid() > 0)
                .mapToInt(e -> e.getPerfFid().intValue())
                .average();

        OptionalDouble avgCls = events.stream()
                .filter(e -> e.getPerfCls() != null && e.getPerfCls() > 0)
                .mapToDouble(e -> e.getPerfCls().doubleValue())
                .average();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("avgFcp", avgFcp.isPresent() ? (int) avgFcp.getAsDouble() : 0);
        result.put("avgLcp", avgLcp.isPresent() ? (int) avgLcp.getAsDouble() : 0);
        result.put("avgFid", avgFid.isPresent() ? (int) avgFid.getAsDouble() : 0);
        result.put("avgCls", avgCls.isPresent() ? Math.round(avgCls.getAsDouble() * 1000.0) / 1000.0 : 0.0);
        result.put("sampleCount", events.size());

        long goodFcp = events.stream().filter(e -> e.getPerfFcp() != null && e.getPerfFcp() <= 1800).count();
        long goodLcp = events.stream().filter(e -> e.getPerfLcp() != null && e.getPerfLcp() <= 2500).count();
        long goodFid = events.stream().filter(e -> e.getPerfFid() != null && e.getPerfFid() <= 100).count();
        long goodCls = events.stream().filter(e -> e.getPerfCls() != null && e.getPerfCls() <= 0.1f).count();

        int total = events.size();
        result.put("fcpGoodRate", total > 0 ? Math.round((double) goodFcp / total * 10000.0) / 100.0 : 0.0);
        result.put("lcpGoodRate", total > 0 ? Math.round((double) goodLcp / total * 10000.0) / 100.0 : 0.0);
        result.put("fidGoodRate", total > 0 ? Math.round((double) goodFid / total * 10000.0) / 100.0 : 0.0);
        result.put("clsGoodRate", total > 0 ? Math.round((double) goodCls / total * 10000.0) / 100.0 : 0.0);

        return result;
    }

    @Override
    public List<Map<String, Object>> getFeatureAdoption(String system, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<AnalyticsActionDaily> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(system), AnalyticsActionDaily::getSystem, system)
                .ge(start != null, AnalyticsActionDaily::getStatDate, start)
                .le(end != null, AnalyticsActionDaily::getStatDate, end);

        List<AnalyticsActionDaily> records = actionDailyMapper.selectList(wrapper);

        Map<String, List<AnalyticsActionDaily>> grouped = records.stream()
                .collect(Collectors.groupingBy(AnalyticsActionDaily::getEventName));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<AnalyticsActionDaily>> entry : grouped.entrySet()) {
            List<AnalyticsActionDaily> actions = entry.getValue();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("featureName", entry.getKey());
            map.put("totalActions", actions.stream().mapToInt(a -> a.getActionCount() != null ? a.getActionCount() : 0).sum());
            map.put("totalUsers", actions.stream().mapToInt(a -> a.getActionUsers() != null ? a.getActionUsers() : 0).sum());
            map.put("totalEnterprises", actions.stream().mapToInt(a -> a.getActionEnterprises() != null ? a.getActionEnterprises() : 0).sum());
            map.put("activeDays", actions.stream().map(AnalyticsActionDaily::getStatDate).distinct().count());
            result.add(map);
        }

        result.sort((a, b) -> Integer.compare(
                (Integer) b.getOrDefault("totalUsers", 0),
                (Integer) a.getOrDefault("totalUsers", 0)));

        return result;
    }

    // ---- Private helper methods ----

    private AnalyticsEvent convertToEntity(AnalyticsCollectDTO.EventDTO event, String ip) {
        AnalyticsEvent entity = new AnalyticsEvent();

        entity.setEventId(event.getEventId() != null ? event.getEventId() : UUID.randomUUID().toString());
        entity.setEventType(event.getEventType() != null ? event.getEventType() : "");
        entity.setEventName(event.getEventName() != null ? event.getEventName() : "");
        entity.setSystem(event.getSystem() != null ? event.getSystem() : "autorecon");
        entity.setIp(ip != null ? ip : "");

        if (event.getTimestamp() != null && event.getTimestamp() > 0) {
            entity.setEventTime(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(event.getTimestamp()), ZoneId.systemDefault()));
        } else {
            entity.setEventTime(LocalDateTime.now());
        }

        Map<String, Object> session = event.getSession();
        if (session != null) {
            entity.setSessionId(strVal(session, "sessionId"));
        }

        Map<String, Object> user = event.getUser();
        if (user != null) {
            entity.setUserId(longVal(user, "userId"));
            entity.setEnterpriseId(longVal(user, "enterpriseId"));
            entity.setRoleType(intVal(user, "roleType"));
            entity.setIsGuest(intVal(user, "isGuest"));
        }

        Map<String, Object> device = event.getDevice();
        if (device != null) {
            entity.setDeviceId(strVal(device, "deviceId"));
            entity.setPlatform(strVal(device, "platform"));
            entity.setOs(strVal(device, "os"));
            entity.setBrowser(strVal(device, "browser"));
            entity.setScreenWidth(shortVal(device, "screenWidth"));
            entity.setScreenHeight(shortVal(device, "screenHeight"));
            entity.setIsMobile(intVal(device, "isMobile"));
        }

        Map<String, Object> page = event.getPage();
        if (page != null) {
            entity.setPagePath(strVal(page, "path"));
            entity.setPageName(strVal(page, "name"));
            entity.setPageTitle(strVal(page, "title"));
            entity.setPageModule(strVal(page, "module"));
            entity.setReferrer(strVal(page, "referrer"));
            entity.setDuration(intVal(page, "duration"));
        }

        Map<String, Object> action = event.getAction();
        if (action != null) {
            entity.setActionCategory(strVal(action, "category"));
            entity.setActionLabel(strVal(action, "label"));
            entity.setActionValue(strVal(action, "value"));
            Object extra = action.get("extra");
            if (extra != null) {
                try {
                    entity.setActionExtra(objectMapper.writeValueAsString(extra));
                } catch (Exception e) {
                    entity.setActionExtra(extra.toString());
                }
            }
        }

        Map<String, Object> perf = event.getPerformance();
        if (perf != null) {
            entity.setPerfFcp(shortVal(perf, "fcp"));
            entity.setPerfLcp(shortVal(perf, "lcp"));
            entity.setPerfFid(shortVal(perf, "fid"));
            entity.setPerfCls(floatVal(perf, "cls"));
        }

        return entity;
    }

    private void updateDailyAggregates(List<AnalyticsEvent> events) {
        Map<String, List<AnalyticsEvent>> pageGroups = events.stream()
                .filter(e -> "page_view".equals(e.getEventType()) && StringUtils.hasText(e.getPagePath()))
                .collect(Collectors.groupingBy(e -> {
                    LocalDate date = e.getEventTime() != null ? e.getEventTime().toLocalDate() : LocalDate.now();
                    return date + "|" + e.getSystem() + "|" + e.getPagePath();
                }));

        for (Map.Entry<String, List<AnalyticsEvent>> entry : pageGroups.entrySet()) {
            List<AnalyticsEvent> group = entry.getValue();
            AnalyticsEvent sample = group.get(0);
            LocalDate date = sample.getEventTime() != null ? sample.getEventTime().toLocalDate() : LocalDate.now();

            LambdaQueryWrapper<AnalyticsPageDaily> qw = new LambdaQueryWrapper<>();
            qw.eq(AnalyticsPageDaily::getStatDate, date)
                    .eq(AnalyticsPageDaily::getSystem, sample.getSystem())
                    .eq(AnalyticsPageDaily::getPagePath, sample.getPagePath());

            AnalyticsPageDaily existing = pageDailyMapper.selectOne(qw);
            if (existing != null) {
                existing.setPv((existing.getPv() != null ? existing.getPv() : 0) + group.size());
                long distinctUsers = group.stream()
                        .map(AnalyticsEvent::getUserId)
                        .filter(Objects::nonNull).filter(uid -> uid > 0)
                        .distinct().count();
                existing.setUv((existing.getUv() != null ? existing.getUv() : 0) + (int) distinctUsers);
                long distinctSessions = group.stream()
                        .map(AnalyticsEvent::getSessionId)
                        .filter(StringUtils::hasText).distinct().count();
                existing.setSessions((existing.getSessions() != null ? existing.getSessions() : 0) + (int) distinctSessions);
                OptionalDouble avgDur = group.stream()
                        .filter(e -> e.getDuration() != null && e.getDuration() > 0)
                        .mapToInt(AnalyticsEvent::getDuration).average();
                if (avgDur.isPresent()) {
                    int oldAvg = existing.getAvgDuration() != null ? existing.getAvgDuration() : 0;
                    existing.setAvgDuration((oldAvg + (int) avgDur.getAsDouble()) / 2);
                }
                existing.setUpdatedAt(LocalDateTime.now());
                pageDailyMapper.updateById(existing);
            } else {
                AnalyticsPageDaily daily = AnalyticsPageDaily.builder()
                        .statDate(date)
                        .system(sample.getSystem())
                        .pagePath(sample.getPagePath())
                        .pageName(sample.getPageName() != null ? sample.getPageName() : "")
                        .pageModule(sample.getPageModule() != null ? sample.getPageModule() : "")
                        .pv(group.size())
                        .uv((int) group.stream().map(AnalyticsEvent::getUserId)
                                .filter(Objects::nonNull).filter(uid -> uid > 0).distinct().count())
                        .sessions((int) group.stream().map(AnalyticsEvent::getSessionId)
                                .filter(StringUtils::hasText).distinct().count())
                        .avgDuration((int) group.stream()
                                .filter(e -> e.getDuration() != null && e.getDuration() > 0)
                                .mapToInt(AnalyticsEvent::getDuration).average().orElse(0))
                        .bounceCount(0)
                        .avgFcp((short) group.stream()
                                .filter(e -> e.getPerfFcp() != null && e.getPerfFcp() > 0)
                                .mapToInt(e -> e.getPerfFcp().intValue()).average().orElse(0))
                        .avgLcp((short) group.stream()
                                .filter(e -> e.getPerfLcp() != null && e.getPerfLcp() > 0)
                                .mapToInt(e -> e.getPerfLcp().intValue()).average().orElse(0))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                pageDailyMapper.insert(daily);
            }
        }

        Map<String, List<AnalyticsEvent>> actionGroups = events.stream()
                .filter(e -> "action".equals(e.getEventType()) && StringUtils.hasText(e.getEventName()))
                .collect(Collectors.groupingBy(e -> {
                    LocalDate date = e.getEventTime() != null ? e.getEventTime().toLocalDate() : LocalDate.now();
                    String cat = e.getActionCategory() != null ? e.getActionCategory() : "";
                    return date + "|" + e.getSystem() + "|" + e.getEventName() + "|" + cat;
                }));

        for (Map.Entry<String, List<AnalyticsEvent>> entry : actionGroups.entrySet()) {
            List<AnalyticsEvent> group = entry.getValue();
            AnalyticsEvent sample = group.get(0);
            LocalDate date = sample.getEventTime() != null ? sample.getEventTime().toLocalDate() : LocalDate.now();
            String category = sample.getActionCategory() != null ? sample.getActionCategory() : "";

            LambdaQueryWrapper<AnalyticsActionDaily> qw = new LambdaQueryWrapper<>();
            qw.eq(AnalyticsActionDaily::getStatDate, date)
                    .eq(AnalyticsActionDaily::getSystem, sample.getSystem())
                    .eq(AnalyticsActionDaily::getEventName, sample.getEventName())
                    .eq(AnalyticsActionDaily::getActionCategory, category);

            AnalyticsActionDaily existing = actionDailyMapper.selectOne(qw);
            if (existing != null) {
                existing.setActionCount((existing.getActionCount() != null ? existing.getActionCount() : 0) + group.size());
                long distinctUsers = group.stream()
                        .map(AnalyticsEvent::getUserId)
                        .filter(Objects::nonNull).filter(uid -> uid > 0).distinct().count();
                existing.setActionUsers((existing.getActionUsers() != null ? existing.getActionUsers() : 0) + (int) distinctUsers);
                long distinctEnterprises = group.stream()
                        .map(AnalyticsEvent::getEnterpriseId)
                        .filter(Objects::nonNull).filter(eid -> eid > 0).distinct().count();
                existing.setActionEnterprises((existing.getActionEnterprises() != null ? existing.getActionEnterprises() : 0) + (int) distinctEnterprises);
                existing.setUpdatedAt(LocalDateTime.now());
                actionDailyMapper.updateById(existing);
            } else {
                AnalyticsActionDaily daily = AnalyticsActionDaily.builder()
                        .statDate(date)
                        .system(sample.getSystem())
                        .eventName(sample.getEventName())
                        .actionCategory(category)
                        .actionCount(group.size())
                        .actionUsers((int) group.stream().map(AnalyticsEvent::getUserId)
                                .filter(Objects::nonNull).filter(uid -> uid > 0).distinct().count())
                        .actionEnterprises((int) group.stream().map(AnalyticsEvent::getEnterpriseId)
                                .filter(Objects::nonNull).filter(eid -> eid > 0).distinct().count())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                actionDailyMapper.insert(daily);
            }
        }
    }

    /**
     * 6-dimension value score:
     * - Traffic (PV weight 15%, UV weight 15%)
     * - Engagement (avgDuration weight 20%)
     * - Quality (bounceRate inversed, weight 20%)
     * - Frequency (sessions/UV ratio, weight 15%)
     * - Reach (UV ratio vs total, weight 15%)
     */
    private double calculateValueScore(int pv, int uv, double avgDuration, int bounceCount, int totalPv) {
        double pvScore = Math.min(pv / 100.0, 1.0) * 15;
        double uvScore = Math.min(uv / 50.0, 1.0) * 15;
        double durationScore = Math.min(avgDuration / 60000.0, 1.0) * 20;
        double bounceRate = totalPv > 0 ? (double) bounceCount / totalPv : 0;
        double qualityScore = (1.0 - bounceRate) * 20;
        double frequencyScore = uv > 0 ? Math.min((double) pv / uv / 5.0, 1.0) * 15 : 0;
        double reachScore = Math.min(uv / 20.0, 1.0) * 15;

        return pvScore + uvScore + durationScore + qualityScore + frequencyScore + reachScore;
    }

    private String assignValueLevel(double score) {
        if (score >= 80) return "S";
        if (score >= 60) return "A";
        if (score >= 40) return "B";
        if (score >= 20) return "C";
        return "D";
    }

    private String strVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }

    private Long longVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try { return Long.parseLong((String) v); } catch (NumberFormatException e) { return 0L; }
        }
        return 0L;
    }

    private Integer intVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private Short shortVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).shortValue();
        if (v instanceof String) {
            try { return Short.parseShort((String) v); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private Float floatVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).floatValue();
        if (v instanceof String) {
            try { return Float.parseFloat((String) v); } catch (NumberFormatException e) { return 0f; }
        }
        return 0f;
    }
}
