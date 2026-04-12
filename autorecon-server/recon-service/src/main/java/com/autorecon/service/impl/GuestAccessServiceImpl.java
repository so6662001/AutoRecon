package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.common.util.TenantUtil;
import com.autorecon.domain.dto.GuestConfirmDTO;
import com.autorecon.domain.vo.GuestBillVO;
import com.autorecon.domain.entity.Dispute;
import com.autorecon.domain.entity.Enterprise;
import com.autorecon.domain.entity.GuestAccessToken;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.enums.BillStatusEnum;
import com.autorecon.mapper.DisputeMapper;
import com.autorecon.mapper.EnterpriseMapper;
import com.autorecon.mapper.GuestAccessTokenMapper;
import com.autorecon.mapper.ReconBillItemMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.GuestAccessService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * 访客访问服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuestAccessServiceImpl implements GuestAccessService {

    private static final int TOKEN_EXPIRE_DAYS = 30;
    private static final int TOKEN_LENGTH = 32;

    private final GuestAccessTokenMapper guestAccessTokenMapper;
    private final ReconBillMapper reconBillMapper;
    private final ReconBillItemMapper reconBillItemMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final DisputeMapper disputeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateGuestToken(Long billId, String buyerPhone) {
        ReconBill bill = reconBillMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        Long currentEnterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (currentEnterpriseId != null && !currentEnterpriseId.equals(bill.getSellerId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        String token = generateRandomToken();
        String phoneHash = hashPhone(buyerPhone);
        LocalDateTime expireAt = LocalDateTime.now().plusDays(TOKEN_EXPIRE_DAYS);

        GuestAccessToken accessToken = GuestAccessToken.builder()
                .token(token)
                .billId(billId)
                .buyerPhoneHash(phoneHash)
                .expireAt(expireAt)
                .phoneVerified(0)
                .openedCount(0)
                .confirmed(0)
                .build();

        guestAccessTokenMapper.insert(accessToken);
        log.info("Generated guest token for billId={}, token={}", billId, token);
        return token;
    }

    private String generateRandomToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[TOKEN_LENGTH];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, TOKEN_LENGTH);
    }

    private String hashPhone(String phone) {
        if (phone == null) return "";
        return DigestUtils.md5DigestAsHex(phone.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GuestBillVO viewBill(String token) {
        LambdaQueryWrapper<GuestAccessToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GuestAccessToken::getToken, token);
        GuestAccessToken accessToken = guestAccessTokenMapper.selectOne(wrapper);
        if (accessToken == null) {
            throw new BizException(ErrorCode.GUEST_TOKEN_INVALID);
        }
        if (accessToken.getExpireAt() != null && accessToken.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.GUEST_TOKEN_EXPIRED);
        }

        LambdaUpdateWrapper<GuestAccessToken> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GuestAccessToken::getId, accessToken.getId())
                .setSql("opened_count = opened_count + 1");
        if (accessToken.getFirstOpenedAt() == null) {
            updateWrapper.set(GuestAccessToken::getFirstOpenedAt, LocalDateTime.now());
        }
        guestAccessTokenMapper.update(null, updateWrapper);

        ReconBill bill = reconBillMapper.selectById(accessToken.getBillId());
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }

        Enterprise seller = enterpriseMapper.selectById(bill.getSellerId());
        Enterprise buyer = enterpriseMapper.selectById(bill.getBuyerId());

        GuestBillVO vo = new GuestBillVO();
        vo.setBillNo(bill.getBillNo());
        vo.setSellerName(seller != null ? seller.getCompanyName() : "");
        vo.setBuyerName(buyer != null ? buyer.getCompanyName() : "");
        vo.setPeriodStart(bill.getPeriodStart());
        vo.setPeriodEnd(bill.getPeriodEnd());
        vo.setTotalAmount(bill.getTotalAmount());
        vo.setCurrentPaymentAmount(bill.getCurrentPaymentAmount());
        vo.setCurrentBalance(bill.getCurrentBalance());
        vo.setPdfUrl(bill.getPdfUrl());
        vo.setConfirmed(accessToken.getConfirmed() != null && accessToken.getConfirmed() == 1);
        vo.setExpired(accessToken.getExpireAt() != null && accessToken.getExpireAt().isBefore(LocalDateTime.now()));

        List<ReconBillItem> items = reconBillItemMapper.selectList(
                new LambdaQueryWrapper<ReconBillItem>().eq(ReconBillItem::getBillId, bill.getId()));
        vo.setItemCount(items != null ? items.size() : 0);
        vo.setItems(items != null ? items : List.of());

        return vo;
    }

    @Override
    public boolean verifyPhone(String token, String code) {
        // TODO: integrate SMS verification
        log.info("verifyPhone placeholder: token={}, code={}", token, code);
        LambdaQueryWrapper<GuestAccessToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GuestAccessToken::getToken, token);
        GuestAccessToken accessToken = guestAccessTokenMapper.selectOne(wrapper);
        if (accessToken != null) {
            LambdaUpdateWrapper<GuestAccessToken> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(GuestAccessToken::getId, accessToken.getId()).set(GuestAccessToken::getPhoneVerified, 1);
            guestAccessTokenMapper.update(null, updateWrapper);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void guestConfirm(GuestConfirmDTO dto) {
        LambdaQueryWrapper<GuestAccessToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GuestAccessToken::getToken, dto.getToken());
        GuestAccessToken accessToken = guestAccessTokenMapper.selectOne(wrapper);
        if (accessToken == null) {
            throw new BizException(ErrorCode.GUEST_TOKEN_INVALID);
        }
        if (accessToken.getExpireAt() != null && accessToken.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.GUEST_TOKEN_EXPIRED);
        }

        if (Boolean.TRUE.equals(dto.getConfirmed())) {
            LambdaUpdateWrapper<GuestAccessToken> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(GuestAccessToken::getId, accessToken.getId())
                    .set(GuestAccessToken::getConfirmed, 1)
                    .set(GuestAccessToken::getConfirmedAt, LocalDateTime.now());
            guestAccessTokenMapper.update(null, updateWrapper);

            ReconBill bill = reconBillMapper.selectById(accessToken.getBillId());
            if (bill != null && BillStatusEnum.PENDING.getCode().equals(bill.getStatus())) {
                bill.setStatus(BillStatusEnum.TO_SIGN.getCode());
                reconBillMapper.updateById(bill);
            }
        } else {
            LambdaUpdateWrapper<GuestAccessToken> disputeUpdateWrapper = new LambdaUpdateWrapper<>();
            disputeUpdateWrapper.eq(GuestAccessToken::getId, accessToken.getId())
                    .set(GuestAccessToken::getDisputeMessage, dto.getDisputeMessage());
            guestAccessTokenMapper.update(null, disputeUpdateWrapper);

            if (dto.getDisputeMessage() != null && !dto.getDisputeMessage().isBlank()) {
                Dispute dispute = Dispute.builder()
                        .billId(accessToken.getBillId())
                        .disputeType(6)
                        .description(dto.getDisputeMessage())
                        .raisedBySide(2)
                        .status(0)
                        .build();
                disputeMapper.insert(dispute);

                ReconBill bill = reconBillMapper.selectById(accessToken.getBillId());
                if (bill != null) {
                    bill.setStatus(BillStatusEnum.DISPUTED.getCode());
                    reconBillMapper.updateById(bill);
                }
            }
        }
        log.info("Guest confirm: token={}, confirmed={}", dto.getToken(), dto.getConfirmed());
    }
}
