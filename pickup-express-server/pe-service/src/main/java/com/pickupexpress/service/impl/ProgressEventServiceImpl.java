package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickupexpress.domain.entity.ProgressEvent;
import com.pickupexpress.mapper.ProgressEventMapper;
import com.pickupexpress.service.ProgressEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 进度事件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ProgressEventServiceImpl extends ServiceImpl<ProgressEventMapper, ProgressEvent>
        implements ProgressEventService {

    @Override
    public void recordEvent(Long pickupOrderId, Long contractId, String eventType, String title, String detail, String operator) {
        ProgressEvent event = ProgressEvent.builder()
                .pickupOrderId(pickupOrderId)
                .contractId(contractId)
                .eventType(eventType)
                .eventTitle(title)
                .eventDetail(detail)
                .operator(operator)
                .notified(0)
                .build();
        save(event);
    }

    @Override
    public List<ProgressEvent> getTimeline(Long pickupOrderId) {
        return list(new LambdaQueryWrapper<ProgressEvent>()
                .eq(ProgressEvent::getPickupOrderId, pickupOrderId)
                .orderByAsc(ProgressEvent::getCreatedAt));
    }

    @Override
    public List<ProgressEvent> getContractTimeline(Long contractId) {
        return list(new LambdaQueryWrapper<ProgressEvent>()
                .eq(ProgressEvent::getContractId, contractId)
                .orderByAsc(ProgressEvent::getCreatedAt));
    }
}
