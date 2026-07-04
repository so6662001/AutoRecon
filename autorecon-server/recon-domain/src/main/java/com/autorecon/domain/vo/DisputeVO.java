package com.autorecon.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异议 VO
 */
@Data
public class DisputeVO {

    private Long id;
    private Long billId;
    private Long billItemId;
    private Integer disputeType;
    private String description;
    private Long raisedBy;
    private Integer raisedBySide;
    private Integer status;
    private LocalDateTime resolvedAt;
    private String resolution;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 扩展字段
    private String billNo;
    private String productName;
    private String spec;
    private String raisedByName;
}
