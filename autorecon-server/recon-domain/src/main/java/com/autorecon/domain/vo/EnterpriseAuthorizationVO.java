package com.autorecon.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 企业数据授权状态（展示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnterpriseAuthorizationVO {

    private Long enterpriseId;
    private String enterpriseName;

    @Builder.Default
    private List<AuthorizationDetail> authorizations = new ArrayList<>();

    @Builder.Default
    private List<ChangeNotification> pendingChanges = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorizationDetail {
        private String type;
        private String typeName;
        private Boolean authorized;
        private LocalDateTime authorizedAt;
        private Boolean revocable;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeNotification {
        private Long id;
        private String changeType;
        private String changeSummary;
        private String newAuthorizationTypes;
        private Integer responseStatus;
        private LocalDateTime notifiedAt;
    }
}
