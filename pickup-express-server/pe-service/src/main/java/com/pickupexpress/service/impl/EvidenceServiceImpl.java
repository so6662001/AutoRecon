package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.common.exception.ErrorCode;
import com.pickupexpress.common.util.TenantUtil;
import com.pickupexpress.domain.entity.*;
import com.pickupexpress.domain.vo.EvidencePackageVO;
import com.pickupexpress.mapper.*;
import com.pickupexpress.service.ProgressEventService;
import com.pickupexpress.service.EvidenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 证据包服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceServiceImpl extends ServiceImpl<EvidencePackageMapper, EvidencePackage> implements EvidenceService {

    private final PickupOrderMapper pickupOrderMapper;
    private final ContractMapper contractMapper;
    private final LiftRecordMapper liftRecordMapper;
    private final DeliveryPhotoMapper deliveryPhotoMapper;
    private final DeliveryConfirmMapper deliveryConfirmMapper;
    private final SettlementOrderMapper settlementOrderMapper;
    private final NotificationLogMapper notificationLogMapper;
    private final ProgressEventService progressEventService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long archiveEvidence(Long pickupOrderId) {
        PickupOrder order = pickupOrderMapper.selectById(pickupOrderId);
        if (order == null) {
            throw new BizException(ErrorCode.PICKUP_ORDER_NOT_FOUND);
        }
        Contract contract = contractMapper.selectById(order.getContractId());
        if (contract != null) {
            TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        }

        StringBuilder content = new StringBuilder();
        if (contract != null) {
            content.append(contract.getContractNo()).append(contract.getSignedPdfUrl());
        }
        content.append(order.getPickupNo()).append(order.getPickupCode());

        List<LiftRecord> lifts = liftRecordMapper.selectList(
                new LambdaQueryWrapper<LiftRecord>().eq(LiftRecord::getPickupOrderId, pickupOrderId));
        lifts.forEach(l -> content.append(l.getId()).append(l.getActualWeight()).append(l.getTheoreticalWeight()));

        List<DeliveryPhoto> photos = deliveryPhotoMapper.selectList(
                new LambdaQueryWrapper<DeliveryPhoto>().eq(DeliveryPhoto::getPickupOrderId, pickupOrderId));
        photos.forEach(p -> content.append(p.getPhotoUrl()));

        DeliveryConfirm confirm = deliveryConfirmMapper.selectOne(
                new LambdaQueryWrapper<DeliveryConfirm>().eq(DeliveryConfirm::getPickupOrderId, pickupOrderId));
        if (confirm != null) {
            content.append(confirm.getSignatureUrl());
        }

        SettlementOrder settlement = settlementOrderMapper.selectOne(
                new LambdaQueryWrapper<SettlementOrder>().eq(SettlementOrder::getPickupOrderId, pickupOrderId));
        if (settlement != null) {
            content.append(settlement.getSettlementNo()).append(settlement.getTotalAmount()).append(settlement.getPdfUrl());
        }

        // 纳入通知记录(设计4.8: 8_通知记录.json)
        List<NotificationLog> notifications = notificationLogMapper.selectList(
                new LambdaQueryWrapper<NotificationLog>()
                        .eq(NotificationLog::getTargetType, "PICKUP_ORDER")
                        .eq(NotificationLog::getTargetId, pickupOrderId));
        for (NotificationLog n : notifications) {
            content.append(n.getId()).append(n.getContent()).append(n.getSentAt());
        }

        String hash = computeSha256(content.toString());

        EvidencePackage pkg = EvidencePackage.builder()
                .pickupOrderId(pickupOrderId)
                .contractId(order.getContractId())
                .packageHash(hash)
                .contractPdfUrl(contract != null ? contract.getSignedPdfUrl() : null)
                .pickupOrderPdfUrl(null) // TODO: 生成提货单PDF
                .deliveryDataUrl(null) // TODO: 发货明细JSON文件URL
                .photosUrls(photos.stream().map(DeliveryPhoto::getPhotoUrl).collect(Collectors.joining(",")))
                .signatureUrl(confirm != null ? confirm.getSignatureUrl() : null)
                .settlementPdfUrl(settlement != null ? settlement.getPdfUrl() : null)
                .evidenceStatus(1)
                .archivedAt(LocalDateTime.now())
                .build();
        save(pkg);

        order.setEvidencePackageId(pkg.getId());
        pickupOrderMapper.updateById(order);

        // 进度事件: 证据归档完成
        progressEventService.recordEvent(order.getId(), order.getContractId(),
                "EVIDENCE_ARCHIVED",
                "证据包归档完成(含合同+提货单+发货" + lifts.size() + "吊+照片" + photos.size() + "张+结算单, 哈希:" + hash.substring(0, 16) + "...)",
                null, "system");

        log.info("Evidence archived: pickupOrderId={}, hash={}", pickupOrderId, hash);
        return pkg.getId();
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public EvidencePackageVO getEvidencePackage(Long pickupOrderId) {
        EvidencePackage pkg = getOne(new LambdaQueryWrapper<EvidencePackage>().eq(EvidencePackage::getPickupOrderId, pickupOrderId));
        if (pkg == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        PickupOrder order = pickupOrderMapper.selectById(pickupOrderId);
        if (order != null) {
            Contract contract = contractMapper.selectById(order.getContractId());
            if (contract != null) {
                TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
            }
        }
        EvidencePackageVO vo = new EvidencePackageVO();
        BeanUtils.copyProperties(pkg, vo);
        if (order != null) {
            vo.setContractNo(order.getContractNo());
            vo.setPickupNo(order.getPickupNo());
        }
        return vo;
    }

    @Override
    public boolean verifyIntegrity(Long evidencePackageId) {
        EvidencePackage pkg = getById(evidencePackageId);
        if (pkg == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        PickupOrder order = pickupOrderMapper.selectById(pkg.getPickupOrderId());
        if (order == null) return false;
        Contract contract = contractMapper.selectById(order.getContractId());
        if (contract != null) {
            TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        }

        StringBuilder content = new StringBuilder();
        if (contract != null) {
            content.append(contract.getContractNo()).append(contract.getSignedPdfUrl());
        }
        content.append(order.getPickupNo()).append(order.getPickupCode());

        List<LiftRecord> lifts = liftRecordMapper.selectList(
                new LambdaQueryWrapper<LiftRecord>().eq(LiftRecord::getPickupOrderId, pkg.getPickupOrderId()));
        lifts.forEach(l -> content.append(l.getId()).append(l.getActualWeight()).append(l.getTheoreticalWeight()));

        List<DeliveryPhoto> photos = deliveryPhotoMapper.selectList(
                new LambdaQueryWrapper<DeliveryPhoto>().eq(DeliveryPhoto::getPickupOrderId, pkg.getPickupOrderId()));
        photos.forEach(ph -> content.append(ph.getPhotoUrl()));

        DeliveryConfirm confirm = deliveryConfirmMapper.selectOne(
                new LambdaQueryWrapper<DeliveryConfirm>().eq(DeliveryConfirm::getPickupOrderId, pkg.getPickupOrderId()));
        if (confirm != null) {
            content.append(confirm.getSignatureUrl());
        }

        SettlementOrder settlement = settlementOrderMapper.selectOne(
                new LambdaQueryWrapper<SettlementOrder>().eq(SettlementOrder::getPickupOrderId, pkg.getPickupOrderId()));
        if (settlement != null) {
            content.append(settlement.getSettlementNo()).append(settlement.getTotalAmount()).append(settlement.getPdfUrl());
        }

        // 通知记录(与archiveEvidence一致)
        List<NotificationLog> notifications = notificationLogMapper.selectList(
                new LambdaQueryWrapper<NotificationLog>()
                        .eq(NotificationLog::getTargetType, "PICKUP_ORDER")
                        .eq(NotificationLog::getTargetId, pkg.getPickupOrderId()));
        for (NotificationLog n : notifications) {
            content.append(n.getId()).append(n.getContent()).append(n.getSentAt());
        }

        String computedHash = computeSha256(content.toString());
        return computedHash.equals(pkg.getPackageHash());
    }

    @Override
    public boolean verifyIntegrityByPickupOrderId(Long pickupOrderId) {
        EvidencePackage pkg = getOne(new LambdaQueryWrapper<EvidencePackage>().eq(EvidencePackage::getPickupOrderId, pickupOrderId));
        if (pkg == null) {
            return false;
        }
        return verifyIntegrity(pkg.getId());
    }
}
