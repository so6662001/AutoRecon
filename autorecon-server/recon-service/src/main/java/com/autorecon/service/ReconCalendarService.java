package com.autorecon.service;

import com.autorecon.domain.dto.CalendarEventDTO;
import com.autorecon.domain.entity.ReconCalendar;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 对账日历服务接口
 */
public interface ReconCalendarService extends IService<ReconCalendar> {

    List<ReconCalendar> getEvents(Long enterpriseId, Integer year, Integer month);

    List<ReconCalendar> getUpcoming(Long enterpriseId, Integer days);

    Long createEvent(CalendarEventDTO dto);

    void updateEventStatus(Long eventId, Integer status);
}
