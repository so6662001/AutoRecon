package com.autorecon.common.util;

import com.autorecon.common.config.AutoReconProperties;
import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;

public final class TenantUtil {
    private TenantUtil() {}

    private static volatile AutoReconProperties autoReconProperties;

    public static void setAutoReconProperties(AutoReconProperties properties) {
        TenantUtil.autoReconProperties = properties;
    }

    private static boolean isDemoMode() {
        return autoReconProperties != null && autoReconProperties.isDemoMode();
    }

    public static void checkBillAccess(Long billSellerId, Long billBuyerId) {
        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId == null) {
            if (!isDemoMode()) {
                throw new BizException(ErrorCode.UNAUTHORIZED);
            }
            return;
        }
        if (!currentEnterpriseId.equals(billSellerId) && !currentEnterpriseId.equals(billBuyerId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }

    public static void checkOwnership(Long resourceEnterpriseId) {
        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId == null) {
            if (!isDemoMode()) {
                throw new BizException(ErrorCode.UNAUTHORIZED);
            }
            return;
        }
        if (!currentEnterpriseId.equals(resourceEnterpriseId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }
}
