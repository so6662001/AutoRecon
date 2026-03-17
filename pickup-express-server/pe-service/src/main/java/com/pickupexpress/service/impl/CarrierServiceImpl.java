package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickupexpress.common.util.SecurityUtil;
import com.pickupexpress.common.util.TenantUtil;
import com.pickupexpress.domain.entity.Carrier;
import com.pickupexpress.mapper.CarrierMapper;
import com.pickupexpress.service.CarrierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 承运公司服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class CarrierServiceImpl extends ServiceImpl<CarrierMapper, Carrier>
        implements CarrierService {

    private static final int STATUS_ACTIVE = 1;

    @Override
    public Long createCarrier(String carrierName, String contactName, String contactPhone) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            throw new com.pickupexpress.common.exception.BizException(com.pickupexpress.common.exception.ErrorCode.UNAUTHORIZED);
        }

        Carrier carrier = Carrier.builder()
                .enterpriseId(enterpriseId)
                .carrierName(carrierName)
                .contactName(contactName)
                .contactPhone(contactPhone)
                .status(STATUS_ACTIVE)
                .build();

        save(carrier);
        return carrier.getId();
    }

    @Override
    public List<Carrier> listCarriers(Long enterpriseId) {
        TenantUtil.checkOwnership(enterpriseId);
        return list(new LambdaQueryWrapper<Carrier>()
                .eq(Carrier::getEnterpriseId, enterpriseId)
                .eq(Carrier::getStatus, STATUS_ACTIVE)
                .orderByDesc(Carrier::getCreatedAt));
    }
}
