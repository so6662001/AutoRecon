package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.result.PageResult;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.FinanceApplyDTO;
import com.autorecon.domain.entity.FinanceApply;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.enums.BillStatusEnum;
import com.autorecon.mapper.FinanceApplyMapper;
import com.autorecon.mapper.ReconBillMapper;
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
        if (!BillStatusEnum.SIGNED.getCode().equals(bill.getStatus())) {
            throw new BizException(ErrorCode.BILL_STATUS_ERROR.getCode(), "仅已签章的对账单可申请融资");
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
        apply.setStatus(status);
        apply.setApprovedAmount(approvedAmount);
        financeApplyMapper.updateById(apply);
        log.info("Updated finance apply status: applyId={}, status={}", applyId, status);
    }
}
