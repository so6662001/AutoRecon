package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.result.PageResult;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.common.util.TenantUtil;
import com.autorecon.domain.dto.FinanceApplyDTO;
import com.autorecon.domain.entity.FinanceApply;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.enums.BillStatusEnum;
import com.autorecon.domain.entity.CreditScore;
import com.autorecon.domain.entity.InvoiceLink;
import com.autorecon.mapper.FinanceApplyMapper;
import com.autorecon.mapper.InvoiceLinkMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.CreditScoreService;
import com.autorecon.service.FinanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 融资服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class FinanceServiceImpl extends ServiceImpl<FinanceApplyMapper, FinanceApply> implements FinanceService {

    private static final DateTimeFormatter APPLY_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final FinanceApplyMapper financeApplyMapper;
    private final ReconBillMapper reconBillMapper;
    private final InvoiceLinkMapper invoiceLinkMapper;
    private final CreditScoreService creditScoreService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(FinanceApplyDTO dto) {
        if (dto == null || dto.getBillId() == null || dto.getApplyAmount() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        Long sellerId = SecurityUtil.getCurrentEnterpriseId();
        if (sellerId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        ReconBill bill = reconBillMapper.selectById(dto.getBillId());
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        // 前提1: 对账单已签章或催收中
        if (!BillStatusEnum.SIGNED.getCode().equals(bill.getStatus())
                && !BillStatusEnum.COLLECTING.getCode().equals(bill.getStatus())) {
            throw new BizException(ErrorCode.BILL_STATUS_ERROR.getCode(), "仅已签章或催收中的对账单可申请融资");
        }

        // 前提2: 已关联发票
        List<InvoiceLink> invoiceLinks = invoiceLinkMapper.selectList(
                new LambdaQueryWrapper<InvoiceLink>().eq(InvoiceLink::getBillId, dto.getBillId()));
        if (invoiceLinks.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "请先关联发票后再申请融资");
        }

        // 前提3: 买方信用评分 ≥ C级 (≥60分)
        try {
            CreditScore buyerScore = creditScoreService.getScore(bill.getBuyerId(), sellerId);
            if (buyerScore != null && buyerScore.getCreditScore() != null
                    && buyerScore.getCreditScore().compareTo(BigDecimal.valueOf(60)) < 0) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(),
                        "买方信用评分不足(当前" + buyerScore.getScoreLevel() + "级)，需C级及以上方可融资");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("无法获取买方信用评分，跳过信用检查: {}", e.getMessage());
        }

        // 前提4: 融资金额 ≤ 应收余额的80%
        BigDecimal maxAmount = bill.getCurrentBalance() != null
                ? bill.getCurrentBalance().multiply(BigDecimal.valueOf(0.8))
                : bill.getTotalAmount().multiply(BigDecimal.valueOf(0.8));
        if (dto.getApplyAmount().compareTo(maxAmount) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(),
                    "融资金额不能超过应收余额的80%(最高可申请¥" + maxAmount.setScale(2, java.math.RoundingMode.HALF_UP) + ")");
        }

        // 构建融资材料汇总(Step 2)
        StringBuilder invoiceInfo = new StringBuilder();
        BigDecimal totalInvoiceAmount = BigDecimal.ZERO;
        for (InvoiceLink link : invoiceLinks) {
            totalInvoiceAmount = totalInvoiceAmount.add(
                    link.getLinkAmount() != null ? link.getLinkAmount() : BigDecimal.ZERO);
            if (link.getInvoiceNo() != null) {
                invoiceInfo.append(link.getInvoiceNo()).append(",");
            }
        }

        String applyNo = "FA" + LocalDateTime.now().format(APPLY_NO_FORMAT);
        FinanceApply apply = FinanceApply.builder()
                .applyNo(applyNo)
                .billId(dto.getBillId())
                .sellerId(sellerId)
                .buyerId(bill.getBuyerId())
                .factorId(dto.getFactorId())
                .applyAmount(dto.getApplyAmount())
                .financeTermDays(dto.getFinanceTermDays())
                .status(1)
                .signedPdfUrl(bill.getSignedPdfUrl())
                .invoiceUrls(invoiceInfo.toString()) // 关联发票号列表
                .build();
        financeApplyMapper.insert(apply);
        log.info("Created finance apply: id={}, applyNo={}", apply.getId(), applyNo);
        return apply.getId();
    }

    @Override
    public FinanceApply getApplyDetail(Long applyId) {
        FinanceApply apply = financeApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "融资申请不存在");
        }
        TenantUtil.checkOwnership(apply.getSellerId());
        return apply;
    }

    @Override
    public PageResult<FinanceApply> listApplies(Long sellerId, Integer status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<FinanceApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinanceApply::getSellerId, sellerId);
        if (status != null) {
            wrapper.eq(FinanceApply::getStatus, status);
        }
        wrapper.orderByDesc(FinanceApply::getCreatedAt);
        Page<FinanceApply> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        IPage<FinanceApply> result = financeApplyMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    @Override
    public List<Long> getEligibleBillIds(Long sellerId) {
        LambdaQueryWrapper<ReconBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconBill::getSellerId, sellerId)
                .and(w -> w.eq(ReconBill::getStatus, BillStatusEnum.SIGNED.getCode())
                        .or().eq(ReconBill::getStatus, BillStatusEnum.COLLECTING.getCode()))
                .isNotNull(ReconBill::getSignedPdfUrl);
        List<ReconBill> bills = reconBillMapper.selectList(wrapper);
        return bills.stream().map(ReconBill::getId).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApplyStatus(Long applyId, Integer status, BigDecimal approvedAmount) {
        FinanceApply apply = financeApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "融资申请不存在");
        }
        TenantUtil.checkOwnership(apply.getSellerId());
        apply.setStatus(status);
        apply.setApprovedAmount(approvedAmount);
        financeApplyMapper.updateById(apply);
        log.info("Updated finance apply status: applyId={}, status={}", applyId, status);
    }
}
