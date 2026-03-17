package com.pickupexpress.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 证据包 VO
 */
@Data
public class EvidencePackageVO {

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String contractNo;
    private String pickupNo;
    private String buyerName;
}
