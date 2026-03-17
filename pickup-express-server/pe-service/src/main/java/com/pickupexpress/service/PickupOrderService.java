package com.pickupexpress.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pickupexpress.domain.dto.DispatchConfirmDTO;
import com.pickupexpress.domain.dto.DispatchRequestDTO;
import com.pickupexpress.domain.dto.DriverAssignDTO;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.vo.PickupOrderDetailVO;
import com.pickupexpress.domain.vo.PickupOrderVO;
import com.pickupexpress.common.result.PageResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * 提货单服务接口
 */
public interface PickupOrderService extends IService<PickupOrder> {

    Long createPickupOrder(DispatchRequestDTO dto);

    PickupOrderDetailVO getPickupOrderDetail(Long id);

    PageResult<PickupOrderVO> queryPickupOrders(Long contractId, Integer status, Integer pageNum, Integer pageSize);

    void confirmDispatch(DispatchConfirmDTO dto);

    void assignDriver(DriverAssignDTO dto);

    void driverAccept(Long pickupOrderId);

    void driverArrive(Long pickupOrderId, BigDecimal lat, BigDecimal lng);

    void cancelPickupOrder(Long id);

    String generatePickupCode();

    List<PickupOrder> listByDriverPhone(String driverPhone);

    PickupOrder getByPickupCode(String pickupCode);
}
