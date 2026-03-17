package com.pickupexpress.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pickupexpress.domain.entity.PickupVerification;
import com.pickupexpress.domain.vo.VerificationResultVO;

/**
 * 提货确权服务
 */
public interface PickupVerificationService extends IService<PickupVerification> {

    VerificationResultVO verify(Long pickupOrderId, String driverName, String driverPhone, String vehiclePlate);

    void sendVerificationSms(Long pickupOrderId);

    void recordPhoneCallResult(Long verificationId, Integer result, String recordingUrl);

    void uploadDriverIdentity(Long verificationId, String idPhotoUrl, String facePhotoUrl, String licensePhotoUrl);

    int determineVerificationLevel(Long buyerId);
}
