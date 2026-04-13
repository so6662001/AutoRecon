package com.autorecon.service;

import com.autorecon.domain.dto.GuestConfirmDTO;
import com.autorecon.domain.vo.GuestBillVO;

/**
 * 访客访问服务接口
 */
public interface GuestAccessService {

    /**
     * 为对账单生成访客访问令牌
     */
    String generateGuestToken(Long billId, String buyerPhone);

    /**
     * 通过访客令牌查看对账单
     */
    GuestBillVO viewBill(String token);

    /**
     * 记录访客访问的 IP 与 User-Agent（由 Controller 调用）
     */
    void recordAccess(String token, String ip, String userAgent);

    /**
     * 验证手机号（访客访问）
     */
    boolean verifyPhone(String token, String code);

    /**
     * 访客确认/异议
     */
    void guestConfirm(GuestConfirmDTO dto);
}
