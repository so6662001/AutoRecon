package com.pickupexpress.service;

import com.pickupexpress.domain.dto.DeliveryCompleteDTO;
import com.pickupexpress.domain.dto.LiftUploadDTO;
import com.pickupexpress.domain.entity.DeliveryPhoto;
import com.pickupexpress.domain.vo.DeliveryProgressVO;

/**
 * 发货服务接口
 */
public interface DeliveryService {

    boolean verifyPickupCode(String pickupCode, String vehiclePlate);

    void uploadLift(LiftUploadDTO dto);

    DeliveryProgressVO getDeliveryProgress(Long pickupOrderId);

    void completeDelivery(DeliveryCompleteDTO dto);

    void uploadPhoto(Long pickupOrderId, DeliveryPhoto photo);
}
