package com.pickupexpress.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pickupexpress.domain.entity.Carrier;

import java.util.List;

/**
 * 承运公司服务
 */
public interface CarrierService extends IService<Carrier> {

    Long createCarrier(String carrierName, String contactName, String contactPhone);

    void updateCarrier(Long id, String carrierName, String contactName, String contactPhone);

    List<Carrier> listCarriers(Long enterpriseId);
}
