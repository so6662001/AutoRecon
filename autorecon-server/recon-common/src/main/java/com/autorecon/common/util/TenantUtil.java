package com.autorecon.common.util;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;

public final class TenantUtil {
    private TenantUtil() {}

    public static void checkBillAccess(Long billSellerId, Long billBuyerId) {
        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId == null) return;
        if (!currentEnterpriseId.equals(billSellerId) && !currentEnterpriseId.equals(billBuyerId)) {
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
