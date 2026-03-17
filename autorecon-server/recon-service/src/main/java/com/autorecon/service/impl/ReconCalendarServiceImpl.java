package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.CalendarEventDTO;
import com.autorecon.domain.entity.ReconCalendar;
import com.autorecon.mapper.ReconCalendarMapper;
import com.autorecon.service.ReconCalendarService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 对账日历服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ReconCalendarServiceImpl extends ServiceImpl<ReconCalendarMapper, ReconCalendar> implements ReconCalendarService {

    private final ReconCalendarMapper reconCalendarMapper;

    @Override
    public List<ReconCalendar> getEvents(Long enterpriseId, Integer year, Integer month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        LambdaQueryWrapper<ReconCalendar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconCalendar::getEnterpriseId, enterpriseId)
                .ge(ReconCalendar::getEventDate, monthStart)
                .le(ReconCalendar::getEventDate, monthEnd)
                .orderByAsc(ReconCalendar::getEventDate);
        return reconCalendarMapper.selectList(wrapper);
    }

    @Override
    public List<ReconCalendar> getUpcoming(Long enterpriseId, Integer days) {
        LocalDate now = LocalDate.now();
        LocalDate endDate = now.plusDays(days);

        LambdaQueryWrapper<ReconCalendar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconCalendar::getEnterpriseId, enterpriseId)
                .ge(ReconCalendar::getEventDate, now)
                .le(ReconCalendar::getEventDate, endDate)
                .eq(ReconCalendar::getStatus, 0)
                .orderByAsc(ReconCalendar::getEventDate);
        return reconCalendarMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEvent(CalendarEventDTO dto) {
        if (dto == null || dto.getEventDate() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        ReconCalendar event = ReconCalendar.builder()
                .enterpriseId(enterpriseId)
                .eventType(dto.getEventType())
                .eventDate(dto.getEventDate())
                .eventTitle(dto.getEventTitle())
                .relatedBillId(dto.getRelatedBillId())
                .relatedBuyerId(dto.getRelatedBuyerId())
                .status(0)
                .build();
        reconCalendarMapper.insert(event);
        log.info("Created calendar event: id={}, enterpriseId={}", event.getId(), enterpriseId);
        return event.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEventStatus(Long eventId, Integer status) {
        ReconCalendar event = reconCalendarMapper.selectById(eventId);
        if (event == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "日历事件不存在");
        }
        event.setStatus(status);
        reconCalendarMapper.updateById(event);
        log.info("Updated calendar event status: eventId={}, status={}", eventId, status);
    }
}
