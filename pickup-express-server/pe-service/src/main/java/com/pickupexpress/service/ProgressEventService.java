package com.pickupexpress.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pickupexpress.domain.entity.ProgressEvent;

import java.util.List;

/**
 * 进度事件服务
 */
public interface ProgressEventService extends IService<ProgressEvent> {

    void recordEvent(Long pickupOrderId, Long contractId, String eventType, String title, String detail, String operator);

    List<ProgressEvent> getTimeline(Long pickupOrderId);

    List<ProgressEvent> getContractTimeline(Long contractId);
}
