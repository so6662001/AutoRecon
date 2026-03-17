package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickupexpress.common.util.TenantUtil;
import com.pickupexpress.domain.dto.PickupPersonRegisterDTO;
import com.pickupexpress.domain.entity.AuthorizedPickupPerson;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.mapper.ContractMapper;
import com.pickupexpress.service.AuthorizedPickupPersonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 授权提货人服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class AuthorizedPickupPersonServiceImpl extends ServiceImpl<com.pickupexpress.mapper.AuthorizedPickupPersonMapper, AuthorizedPickupPerson>
        implements AuthorizedPickupPersonService {

    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_DISABLED = 0;
    private static final int BUYER_CONFIRMED_YES = 1;
    private static final int REGISTERED_BY_CUSTOMER = 1;

    private final ContractMapper contractMapper;

    @Override
    public Long register(PickupPersonRegisterDTO dto) {
        Contract contract = dto.getContractId() != null ? contractMapper.selectById(dto.getContractId()) : null;
        if (contract != null) {
            TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        }

        int buyerConfirmed = REGISTERED_BY_CUSTOMER == dto.getRegisteredBy() ? 1 : 0;
        LocalDate validFrom = LocalDate.now();
        LocalDate validUntil = validFrom.plusYears(1);

        AuthorizedPickupPerson person = AuthorizedPickupPerson.builder()
                .buyerId(dto.getBuyerId())
                .contractId(dto.getContractId())
                .personName(dto.getPersonName())
                .idCardNo(dto.getIdCardNo())
                .phone(dto.getPhone())
                .vehiclePlate(dto.getVehiclePlate())
                .maxPickupWeight(dto.getMaxPickupWeight())
                .registeredBy(dto.getRegisteredBy())
                .buyerConfirmed(buyerConfirmed)
                .buyerConfirmedAt(buyerConfirmed == 1 ? LocalDateTime.now() : null)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .status(STATUS_ACTIVE)
                .build();

        save(person);
        return person.getId();
    }

    @Override
    public List<AuthorizedPickupPerson> listByBuyer(Long buyerId) {
        return list(new LambdaQueryWrapper<AuthorizedPickupPerson>()
                .eq(AuthorizedPickupPerson::getBuyerId, buyerId)
                .eq(AuthorizedPickupPerson::getStatus, STATUS_ACTIVE)
                .orderByDesc(AuthorizedPickupPerson::getCreatedAt));
    }

    @Override
    public boolean isAuthorized(Long buyerId, String driverPhone) {
        LocalDate today = LocalDate.now();
        long count = count(new LambdaQueryWrapper<AuthorizedPickupPerson>()
                .eq(AuthorizedPickupPerson::getBuyerId, buyerId)
                .eq(AuthorizedPickupPerson::getPhone, driverPhone)
                .eq(AuthorizedPickupPerson::getStatus, STATUS_ACTIVE)
                .le(AuthorizedPickupPerson::getValidFrom, today)
                .ge(AuthorizedPickupPerson::getValidUntil, today));
        return count > 0;
    }

    @Override
    public void confirmByBuyer(Long id) {
        AuthorizedPickupPerson person = getById(id);
        if (person == null) return;

        person.setBuyerConfirmed(BUYER_CONFIRMED_YES);
        person.setBuyerConfirmedAt(LocalDateTime.now());
        updateById(person);
    }

    @Override
    public void disable(Long id) {
        AuthorizedPickupPerson person = getById(id);
        if (person == null) return;

        person.setStatus(STATUS_DISABLED);
        updateById(person);
    }
}
