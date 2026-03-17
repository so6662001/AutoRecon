package com.pickupexpress.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pickupexpress.domain.dto.WarehouseCreateDTO;
import com.pickupexpress.domain.entity.Warehouse;

import java.util.List;

/**
 * 仓库服务接口
 */
public interface WarehouseService extends IService<Warehouse> {

    Long createWarehouse(WarehouseCreateDTO dto);

    void updateWarehouse(Long id, WarehouseCreateDTO dto);

    List<Warehouse> listWarehouses(Long enterpriseId);
}
