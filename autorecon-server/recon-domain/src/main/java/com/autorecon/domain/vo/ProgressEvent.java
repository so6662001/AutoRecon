package com.autorecon.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 自动对账计划执行进度事件（由审计日志映射）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressEvent {

    private Long id;
    private String module;
    private String action;
    private String detail;
    private LocalDateTime createdAt;
}
