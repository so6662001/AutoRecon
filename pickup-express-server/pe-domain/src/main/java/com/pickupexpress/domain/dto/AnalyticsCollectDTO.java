package com.pickupexpress.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AnalyticsCollectDTO {

    @NotEmpty(message = "事件列表不能为空")
    @Valid
    private List<EventDTO> events;

    @Data
    public static class EventDTO {
        private String eventId;
        private String eventType;
        private String eventName;
        private Long timestamp;
        private String system;
        private Map<String, Object> session;
        private Map<String, Object> user;
        private Map<String, Object> device;
        private Map<String, Object> page;
        private Map<String, Object> action;
        private Map<String, Object> performance;
    }
}
