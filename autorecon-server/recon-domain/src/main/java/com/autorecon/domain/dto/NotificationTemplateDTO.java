package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class NotificationTemplateDTO {

    private List<Item> templates;

    @Data
    public static class Item {
        private String id;
        @NotBlank
        private String eventType;
        /** SMS, EMAIL, PUSH, etc. — comma-separated channels summary for table */
        private String channels;
        private String smsTemplate;
        private String emailTemplate;
        private String pushTemplate;
        /** ENABLED / DISABLED */
        private String status;
    }
}
