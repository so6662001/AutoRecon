package com.autorecon.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class TimeoutConfigDTO {

    @Valid
    @NotNull
    private GlobalTimeout global;

    @Valid
    private List<CustomerTimeout> customers;

    @Data
    public static class GlobalTimeout {
        @Min(1)
        @Max(7)
        private Integer noDiffDays = 3;
        @Min(12)
        @Max(72)
        private Integer specChangeHours = 24;
        @Min(24)
        @Max(72)
        private Integer overDiffHours = 48;
        @Min(1)
        @Max(7)
        private Integer settleDays = 3;
        @Min(1)
        @Max(48)
        private Integer reminderHours = 6;
    }

    @Data
    public static class CustomerTimeout {
        private String id;
        private Long buyerId;
        private String buyerName;
        @Min(1)
        @Max(7)
        private Integer noDiffDays;
        @Min(12)
        @Max(72)
        private Integer specChangeHours;
        @Min(24)
        @Max(72)
        private Integer overDiffHours;
        @Min(1)
        @Max(7)
        private Integer settleDays;
    }
}
