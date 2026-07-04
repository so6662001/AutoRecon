package com.pickupexpress.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 证据包
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("evidence_package")
public class EvidencePackage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long pickupOrderId;
    private Long contractId;
    private String packageHash;
    private String contractPdfUrl;
    private String pickupOrderPdfUrl;
    private String deliveryDataUrl;
    private String photosUrls;
    private String signatureUrl;
    private String settlementPdfUrl;
    private String blockchainHash;
    private Integer evidenceStatus;
    private LocalDateTime archivedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
