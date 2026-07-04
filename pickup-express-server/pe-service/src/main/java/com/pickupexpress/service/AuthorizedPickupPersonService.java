package com.pickupexpress.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pickupexpress.domain.dto.PickupPersonRegisterDTO;
import com.pickupexpress.domain.entity.AuthorizedPickupPerson;

import java.util.List;

/**
 * 授权提货人服务
 */
public interface AuthorizedPickupPersonService extends IService<AuthorizedPickupPerson> {

    Long register(PickupPersonRegisterDTO dto);

    List<AuthorizedPickupPerson> listByBuyer(Long buyerId);

    boolean isAuthorized(Long buyerId, String driverPhone);

    void confirmByBuyer(Long id);

    void disable(Long id);
}
