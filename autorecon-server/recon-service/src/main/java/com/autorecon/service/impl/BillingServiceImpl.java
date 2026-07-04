package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.result.PageResult;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.SubscribeDTO;
import com.autorecon.domain.entity.BillingRecord;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.entity.ServiceUsage;
import com.autorecon.domain.entity.Subscription;
import com.autorecon.domain.vo.UsageVO;
import com.autorecon.mapper.BillingRecordMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.mapper.ServiceUsageMapper;
import com.autorecon.mapper.SubscriptionMapper;
import com.autorecon.service.BillingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 计费服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private static final int SERVICE_TYPE_SEAL = 1;

    private final SubscriptionMapper subscriptionMapper;
    private final BillingRecordMapper billingRecordMapper;
    private final ServiceUsageMapper serviceUsageMapper;
    private final ReconBillMapper reconBillMapper;

    @Override
    public Subscription getCurrentSubscription(Long enterpriseId) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        LambdaQueryWrapper<Subscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Subscription::getEnterpriseId, eid)
                .eq(Subscription::getStatus, 1)
                .orderByDesc(Subscription::getCreatedAt)
                .last("LIMIT 1");
        return subscriptionMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void subscribe(Long enterpriseId, SubscribeDTO dto) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        if (eid == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        BigDecimal actualPrice = calculatePrice(dto.getPlanType(), dto.getBillingCycle());
        LocalDate today = LocalDate.now();
        LocalDate endDate = dto.getBillingCycle() != null && dto.getBillingCycle() == 2
                ? today.plusYears(1)
                : today.plusMonths(1);

        LambdaQueryWrapper<Subscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Subscription::getEnterpriseId, eid).orderByDesc(Subscription::getCreatedAt).last("LIMIT 1");
        Subscription sub = subscriptionMapper.selectOne(wrapper);
        if (sub == null) {
            sub = Subscription.builder()
                    .enterpriseId(eid)
                    .planType(dto.getPlanType() != null ? dto.getPlanType() : 1)
                    .billingCycle(dto.getBillingCycle() != null ? dto.getBillingCycle() : 1)
                    .unitPrice(actualPrice)
                    .discountRate(BigDecimal.ZERO)
                    .actualPrice(actualPrice)
                    .startDate(today)
                    .endDate(endDate)
                    .autoRenew(1)
                    .status(1)
                    .referralCode(dto.getReferralCode())
                    .build();
            subscriptionMapper.insert(sub);
        } else {
            sub.setPlanType(dto.getPlanType() != null ? dto.getPlanType() : sub.getPlanType());
            sub.setBillingCycle(dto.getBillingCycle() != null ? dto.getBillingCycle() : sub.getBillingCycle());
            sub.setActualPrice(actualPrice);
            sub.setStartDate(today);
            sub.setEndDate(endDate);
            sub.setAutoRenew(1);
            sub.setStatus(1);
            sub.setReferralCode(dto.getReferralCode());
            subscriptionMapper.updateById(sub);
        }
        log.info("Subscribed: enterpriseId={}, planType={}", eid, sub.getPlanType());
    }

    private BigDecimal calculatePrice(Integer planType, Integer billingCycle) {
        if (planType == null) planType = 1;
        if (billingCycle == null) billingCycle = 1;
        BigDecimal base = switch (planType) {
            case 2 -> new BigDecimal("999");
            case 3 -> new BigDecimal("2999");
            default -> new BigDecimal("299");
        };
        return billingCycle == 2 ? base.multiply(new BigDecimal("10")) : base;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSubscription(Long enterpriseId) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        Subscription sub = getCurrentSubscription(eid);
        if (sub == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "当前无有效订阅");
        }
        sub.setAutoRenew(0);
        subscriptionMapper.updateById(sub);
        log.info("Cancelled subscription: enterpriseId={}", eid);
    }

    @Override
    public UsageVO getUsage(Long enterpriseId) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        UsageVO vo = new UsageVO();
        vo.setPlanType(1);
        vo.setPlanName("基础版");

        Subscription sub = getCurrentSubscription(eid);
        if (sub != null) {
            vo.setPlanType(sub.getPlanType());
            vo.setPlanName(switch (sub.getPlanType() != null ? sub.getPlanType() : 1) {
                case 2 -> "专业版";
                case 3 -> "企业版";
                default -> "基础版";
            });
        }

        YearMonth ym = YearMonth.now();
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        LambdaQueryWrapper<ReconBill> billWrapper = new LambdaQueryWrapper<>();
        billWrapper.eq(ReconBill::getSellerId, eid)
                .ge(ReconBill::getCreatedAt, monthStart.atStartOfDay())
                .le(ReconBill::getCreatedAt, monthEnd.plusDays(1).atStartOfDay());
        long billCount = reconBillMapper.selectCount(billWrapper);
        vo.setBillCount((int) billCount);
        vo.setBillLimit(100);

        List<ReconBill> bills = reconBillMapper.selectList(billWrapper);
        long clientCount = bills.stream().map(ReconBill::getBuyerId).distinct().count();
        vo.setClientCount((int) clientCount);
        vo.setClientLimit(50);

        LambdaQueryWrapper<ServiceUsage> usageWrapper = new LambdaQueryWrapper<>();
        usageWrapper.eq(ServiceUsage::getEnterpriseId, eid)
                .eq(ServiceUsage::getServiceType, SERVICE_TYPE_SEAL)
                .eq(ServiceUsage::getUsageMonth, ym.toString())
                .last("LIMIT 1");
        ServiceUsage sealUsage = serviceUsageMapper.selectOne(usageWrapper);
        if (sealUsage != null) {
            vo.setSealUsed(sealUsage.getUsedCount() != null ? sealUsage.getUsedCount() : 0);
            vo.setSealRemaining(sealUsage.getQuotaRemaining() != null ? sealUsage.getQuotaRemaining() : 0);
        } else {
            vo.setSealUsed(0);
            vo.setSealRemaining(0);
        }
        vo.setStorageUsedMb(0L);
        return vo;
    }

    @Override
    public PageResult<BillingRecord> getBills(Long enterpriseId, Integer pageNum, Integer pageSize) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        LambdaQueryWrapper<BillingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BillingRecord::getEnterpriseId, eid).orderByDesc(BillingRecord::getCreatedAt);

        int pn = pageNum != null && pageNum > 0 ? pageNum : 1;
        int ps = pageSize != null && pageSize > 0 ? pageSize : 10;
        Page<BillingRecord> page = new Page<>(pn, ps);
        IPage<BillingRecord> result = billingRecordMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purchaseSealPackage(Long enterpriseId, Integer packageType) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        if (eid == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        int quotaToAdd = packageType != null && packageType == 2 ? 100 : 50;
        String usageMonth = YearMonth.now().toString();

        LambdaQueryWrapper<ServiceUsage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceUsage::getEnterpriseId, eid)
                .eq(ServiceUsage::getServiceType, SERVICE_TYPE_SEAL)
                .eq(ServiceUsage::getUsageMonth, usageMonth)
                .last("LIMIT 1");
        ServiceUsage usage = serviceUsageMapper.selectOne(wrapper);
        if (usage == null) {
            usage = ServiceUsage.builder()
                    .enterpriseId(eid)
                    .serviceType(SERVICE_TYPE_SEAL)
                    .usageMonth(usageMonth)
                    .usedCount(0)
                    .quotaTotal(quotaToAdd)
                    .quotaRemaining(quotaToAdd)
                    .amount(BigDecimal.ZERO)
                    .build();
            serviceUsageMapper.insert(usage);
        } else {
            usage.setQuotaTotal((usage.getQuotaTotal() != null ? usage.getQuotaTotal() : 0) + quotaToAdd);
            usage.setQuotaRemaining((usage.getQuotaRemaining() != null ? usage.getQuotaRemaining() : 0) + quotaToAdd);
            serviceUsageMapper.updateById(usage);
        }
        log.info("Purchased seal package: enterpriseId={}, packageType={}, quotaAdded={}", eid, packageType, quotaToAdd);
    }

    @Override
    public Integer getSealQuota(Long enterpriseId) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        String usageMonth = YearMonth.now().toString();
        LambdaQueryWrapper<ServiceUsage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceUsage::getEnterpriseId, eid)
                .eq(ServiceUsage::getServiceType, SERVICE_TYPE_SEAL)
                .eq(ServiceUsage::getUsageMonth, usageMonth)
                .last("LIMIT 1");
        ServiceUsage usage = serviceUsageMapper.selectOne(wrapper);
        return usage != null && usage.getQuotaRemaining() != null ? usage.getQuotaRemaining() : 0;
    }
}
