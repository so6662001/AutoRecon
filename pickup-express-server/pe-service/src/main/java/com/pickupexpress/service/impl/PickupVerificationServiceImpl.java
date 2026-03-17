package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.common.exception.ErrorCode;
import com.pickupexpress.common.util.TenantUtil;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.entity.PickupVerification;
import com.pickupexpress.domain.enums.VerificationLevelEnum;
import com.pickupexpress.domain.vo.VerificationResultVO;
import com.pickupexpress.mapper.ContractMapper;
import com.pickupexpress.mapper.PickupOrderMapper;
import com.pickupexpress.mapper.PickupVerificationMapper;
import com.pickupexpress.service.AuthorizedPickupPersonService;
import com.pickupexpress.service.NotificationService;
import com.pickupexpress.service.PickupVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 提货确权服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PickupVerificationServiceImpl extends ServiceImpl<PickupVerificationMapper, PickupVerification>
        implements PickupVerificationService {

    private final PickupOrderMapper pickupOrderMapper;
    private final ContractMapper contractMapper;
    private final AuthorizedPickupPersonService authorizedPickupPersonService;
    private final NotificationService notificationService;

    @Override
    public VerificationResultVO verify(Long pickupOrderId, String driverName, String driverPhone, String vehiclePlate) {
        PickupOrder order = pickupOrderMapper.selectById(pickupOrderId);
        if (order == null) {
            throw new BizException(ErrorCode.PICKUP_ORDER_NOT_FOUND);
        }
        Contract contract = contractMapper.selectById(order.getContractId());
        if (contract != null) {
            TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        }

        Long buyerId = order.getBuyerId();
        Long contractId = order.getContractId();

        boolean isPreRegistered = authorizedPickupPersonService.isAuthorized(buyerId, driverPhone);
        int level = determineVerificationLevel(buyerId);

        if (!isPreRegistered && level >= VerificationLevelEnum.ENHANCED.getValue()) {
            log.info("Triggering phone call for verification, pickupOrderId={}, driverPhone={}", pickupOrderId, driverPhone);
        }

        PickupVerification verification = PickupVerification.builder()
                .pickupOrderId(pickupOrderId)
                .contractId(contractId)
                .buyerId(buyerId)
                .driverName(driverName)
                .driverPhone(driverPhone)
                .vehiclePlate(vehiclePlate)
                .isPreRegistered(isPreRegistered ? 1 : 0)
                .verificationLevel(level)
                .verificationResult(0)
                .smsSent(0)
                .phoneCallMade(isPreRegistered ? 0 : (level >= VerificationLevelEnum.ENHANCED.getValue() ? 1 : 0))
                .build();

        save(verification);

        sendVerificationSms(pickupOrderId);

        verification.setSmsSent(1);
        verification.setSmsSentAt(LocalDateTime.now());
        updateById(verification);

        return VerificationResultVO.builder()
                .verificationId(verification.getId())
                .pickupOrderId(pickupOrderId)
                .isPreRegistered(isPreRegistered)
                .verificationLevel(level)
                .verificationResult(0)
                .message(isPreRegistered ? "预登记司机，标准确权" : "需加强确权")
                .build();
    }

    @Override
    public void sendVerificationSms(Long pickupOrderId) {
        PickupOrder order = pickupOrderMapper.selectById(pickupOrderId);
        if (order == null) return;

        String content = String.format("提货通知：合同%s，司机%s，车牌%s，请确认提货信息。",
                order.getContractNo(), order.getDriverName(), order.getVehiclePlate());

        notificationService.sendVerificationSms(pickupOrderId, order.getDriverPhone(), content);
    }

    @Override
    public void recordPhoneCallResult(Long verificationId, Integer result, String recordingUrl) {
        PickupVerification verification = getById(verificationId);
        if (verification == null) return;

        verification.setPhoneCallResult(result);
        verification.setPhoneCallRecordingUrl(recordingUrl);
        verification.setPhoneCallAt(LocalDateTime.now());
        updateById(verification);
    }

    @Override
    public void uploadDriverIdentity(Long verificationId, String idPhotoUrl, String facePhotoUrl, String licensePhotoUrl) {
        PickupVerification verification = getById(verificationId);
        if (verification == null) return;

        verification.setDriverIdPhotoUrl(idPhotoUrl);
        verification.setDriverFacePhotoUrl(facePhotoUrl);
        verification.setDriverLicensePhotoUrl(licensePhotoUrl);
        updateById(verification);
    }

    @Override
    public int determineVerificationLevel(Long buyerId) {
        // Placeholder: query buyer credit score from AutoRecon if connected
        // New buyer = ENHANCED
        // Based on pickup amount: >500K = STRICT
        return VerificationLevelEnum.ENHANCED.getValue();
    }
}
