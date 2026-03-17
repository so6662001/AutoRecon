package com.pickupexpress.common.util;

import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.common.exception.ErrorCode;

public final class TenantUtil {
    private TenantUtil() {}

    public static void checkContractAccess(Long contractSellerId, Long contractBuyerId) {
        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId == null) return;
        if (!currentEnterpriseId.equals(contractSellerId) && !currentEnterpriseId.equals(contractBuyerId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }

    public static void checkOwnership(Long resourceEnterpriseId) {
        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId == null) return;
        if (!currentEnterpriseId.equals(resourceEnterpriseId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }
}
