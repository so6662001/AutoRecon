package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.common.exception.ErrorCode;
import com.pickupexpress.common.util.SecurityUtil;
import com.pickupexpress.common.util.TenantUtil;
import com.pickupexpress.domain.dto.WarehouseCreateDTO;
import com.pickupexpress.domain.entity.Warehouse;
import com.pickupexpress.mapper.WarehouseMapper;
import com.pickupexpress.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 仓库服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, Warehouse> implements WarehouseService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWarehouse(WarehouseCreateDTO dto) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        Warehouse warehouse = Warehouse.builder()
                .enterpriseId(enterpriseId)
                .warehouseName(dto.getWarehouseName())
                .address(dto.getAddress())
                .contactName(dto.getContactName())
                .contactPhone(dto.getContactPhone())
                .deliveryMode(dto.getDefaultDeliveryMode())
                .defaultDeliveryMode(dto.getDefaultDeliveryMode())
                .hasWms(dto.getHasWms())
                .wmsConfig(dto.getHasWms() != null && dto.getHasWms() == 1 ? "{}" : null)
                .status(1)
                .build();
        save(warehouse);
        return warehouse.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouse(Long id, WarehouseCreateDTO dto) {
        Warehouse warehouse = getById(id);
        if (warehouse == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        TenantUtil.checkOwnership(warehouse.getEnterpriseId());

        warehouse.setWarehouseName(dto.getWarehouseName());
        warehouse.setAddress(dto.getAddress());
        warehouse.setContactName(dto.getContactName());
        warehouse.setContactPhone(dto.getContactPhone());
        warehouse.setDeliveryMode(dto.getDefaultDeliveryMode());
        warehouse.setDefaultDeliveryMode(dto.getDefaultDeliveryMode());
        warehouse.setHasWms(dto.getHasWms());
        warehouse.setWmsConfig(dto.getHasWms() != null && dto.getHasWms() == 1 ? "{}" : warehouse.getWmsConfig());
        updateById(warehouse);
    }

    @Override
    public List<Warehouse> listWarehouses(Long enterpriseId) {
        return list(new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getEnterpriseId, enterpriseId)
                .orderByDesc(Warehouse::getCreatedAt));
    }
}
