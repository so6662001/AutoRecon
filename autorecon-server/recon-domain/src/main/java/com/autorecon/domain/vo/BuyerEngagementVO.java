package com.autorecon.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 买方参与度 VO
 */
@Data
public class BuyerEngagementVO {

    private Long id;
    private Long buyerEnterpriseId;
    private Long sellerEnterpriseId;
    private String buyerPhone;
    private String buyerContactName;
    private String buyerCompanyName;
    private Integer engagementLevel;
    private LocalDateTime firstLinkSentAt;
    private LocalDateTime firstLinkOpenedAt;
    private LocalDateTime registeredAt;
    private LocalDateTime firstDataSubmitAt;
    private LocalDateTime sealInitializedAt;
    private LocalDateTime erpConnectedAt;
    private Integer totalBillsSent;
    private Integer totalBillsConfirmed;
    private Integer totalBillsIgnored;
    private LocalDateTime lastActiveAt;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String suggestedAction;
}
