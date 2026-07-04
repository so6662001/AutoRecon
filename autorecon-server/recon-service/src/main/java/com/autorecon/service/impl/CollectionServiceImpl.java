package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.result.PageResult;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.CollectionPlanCreateDTO;
import com.autorecon.domain.entity.CollectionLog;
import com.autorecon.domain.entity.CollectionPlan;
import com.autorecon.domain.entity.CreditScore;
import com.autorecon.domain.entity.Enterprise;
import com.autorecon.domain.entity.ReconBill;
import com.autorecon.domain.enums.BillStatusEnum;
import com.autorecon.domain.vo.CollectionPlanVO;
import com.autorecon.mapper.CollectionLogMapper;
import com.autorecon.mapper.CollectionPlanMapper;
import com.autorecon.mapper.EnterpriseMapper;
import com.autorecon.mapper.ReconBillMapper;
import com.autorecon.service.CollectionService;
import com.autorecon.service.CreditScoreService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 催收计划服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionServiceImpl extends ServiceImpl<CollectionPlanMapper, CollectionPlan> implements CollectionService {

    private final CollectionPlanMapper collectionPlanMapper;
    private final CollectionLogMapper collectionLogMapper;
    private final ReconBillMapper reconBillMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final CreditScoreService creditScoreService;

    private static int mapActionType(String actionType) {
        if (actionType == null) return 1;
        return switch (actionType.toLowerCase()) {
            case "call" -> 1;
            case "sms" -> 2;
            case "visit" -> 3;
            case "letter" -> 4;
            default -> 1;
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPlan(CollectionPlanCreateDTO dto) {
        ReconBill bill = reconBillMapper.selectById(dto.getBillId());
        if (bill == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }

        BigDecimal receivable = dto.getReceivableAmount() != null ? dto.getReceivableAmount() : bill.getCurrentBalance();
        if (receivable == null) receivable = BigDecimal.ZERO;

        CollectionPlan plan = CollectionPlan.builder()
                .billId(dto.getBillId())
                .sellerId(dto.getSellerId())
                .buyerId(dto.getBuyerId())
                .receivableAmount(receivable)
                .collectedAmount(BigDecimal.ZERO)
                .remainingAmount(receivable)
                .dueDate(dto.getDueDate())
                .strategyLevel(dto.getStrategyLevel())
                .status(1)
                .currentStage(1)
                .nextActionDate(dto.getDueDate())
                .build();

        collectionPlanMapper.insert(plan);
        log.info("Created collection plan: id={}, billId={}", plan.getId(), dto.getBillId());
        return plan.getId();
    }

    @Override
    public PageResult<CollectionPlanVO> listPlans(Long sellerId, Integer status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<CollectionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollectionPlan::getSellerId, sellerId);
        if (status != null) {
            wrapper.eq(CollectionPlan::getStatus, status);
        }
        wrapper.orderByDesc(CollectionPlan::getCreatedAt);

        int pn = pageNum != null && pageNum > 0 ? pageNum : 1;
        int ps = pageSize != null && pageSize > 0 ? pageSize : 10;
        Page<CollectionPlan> page = new Page<>(pn, ps);
        IPage<CollectionPlan> result = collectionPlanMapper.selectPage(page, wrapper);

        List<CollectionPlanVO> voList = result.getRecords().stream().map(plan -> {
            CollectionPlanVO vo = new CollectionPlanVO();
            BeanUtils.copyProperties(plan, vo);
            ReconBill bill = reconBillMapper.selectById(plan.getBillId());
            if (bill != null) {
                vo.setBillNo(bill.getBillNo());
                if (plan.getDueDate() != null) {
                    long days = ChronoUnit.DAYS.between(plan.getDueDate(), LocalDate.now());
                    vo.setOverdueDays(days > 0 ? (int) days : 0);
                }
            }
            Enterprise seller = enterpriseMapper.selectById(plan.getSellerId());
            if (seller != null) vo.setSellerName(seller.getCompanyName());
            Enterprise buyer = enterpriseMapper.selectById(plan.getBuyerId());
            if (buyer != null) vo.setBuyerName(buyer.getCompanyName());
            return vo;
        }).toList();

        return PageResult.of(new Page<CollectionPlanVO>().setRecords(voList).setTotal(result.getTotal())
                .setSize(result.getSize()).setCurrent(result.getCurrent()).setPages(result.getPages()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executePlan(Long planId, String actionType, String content) {
        CollectionPlan plan = collectionPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "催收计划不存在");
        }

        CollectionLog logEntry = CollectionLog.builder()
                .planId(planId)
                .billId(plan.getBillId())
                .actionType(mapActionType(actionType))
                .actionStage(plan.getCurrentStage())
                .content(content)
                .executedAt(LocalDateTime.now())
                .executedBy(String.valueOf(SecurityUtil.getCurrentUserId()))
                .build();

        collectionLogMapper.insert(logEntry);

        plan.setNextActionDate(LocalDate.now().plusDays(1));
        collectionPlanMapper.updateById(plan);

        log.info("Executed collection plan: planId={}, actionType={}", planId, actionType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerPayment(Long planId, BigDecimal amount, String remark) {
        CollectionPlan plan = collectionPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "催收计划不存在");
        }

        BigDecimal collected = plan.getCollectedAmount() != null ? plan.getCollectedAmount() : BigDecimal.ZERO;
        BigDecimal remaining = plan.getRemainingAmount() != null ? plan.getRemainingAmount() : plan.getReceivableAmount();
        if (remaining == null) remaining = plan.getReceivableAmount();

        collected = collected.add(amount);
        remaining = remaining.subtract(amount);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

        plan.setCollectedAmount(collected);
        plan.setRemainingAmount(remaining);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            plan.setStatus(3);
            ReconBill bill = reconBillMapper.selectById(plan.getBillId());
            if (bill != null) {
                bill.setStatus(BillStatusEnum.COMPLETED.getCode());
                reconBillMapper.updateById(bill);
                log.info("Bill {} completed via collection payment", bill.getId());
            }

            try {
                creditScoreService.recalculateScore(plan.getBuyerId(), plan.getSellerId());
            } catch (Exception e) {
                log.warn("Failed to recalculate credit score after payment", e);
            }
        }

        collectionPlanMapper.updateById(plan);
        log.info("Registered payment: planId={}, amount={}", planId, amount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoCreatePlanForBill(ReconBill bill) {
        if (bill.getCurrentBalance() == null || bill.getCurrentBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        CollectionPlan existing = getOne(new LambdaQueryWrapper<CollectionPlan>()
                .eq(CollectionPlan::getBillId, bill.getId()));
        if (existing != null) {
            return;
        }

        String strategy = "B";
        try {
            CreditScore creditScore = creditScoreService.getScore(bill.getBuyerId(), bill.getSellerId());
            if (creditScore != null && creditScore.getScoreLevel() != null) {
                strategy = creditScore.getScoreLevel();
            }
        } catch (Exception e) {
            log.warn("Failed to get credit score for strategy", e);
        }

        CollectionPlan plan = CollectionPlan.builder()
                .billId(bill.getId())
                .sellerId(bill.getSellerId())
                .buyerId(bill.getBuyerId())
                .receivableAmount(bill.getCurrentBalance())
                .collectedAmount(BigDecimal.ZERO)
                .remainingAmount(bill.getCurrentBalance())
                .dueDate(LocalDate.now().plusDays(30))
                .strategyLevel(strategy)
                .status(1)
                .currentStage(1)
                .nextActionDate(LocalDate.now().plusDays(7))
                .build();
        save(plan);
        log.info("Auto-created collection plan for bill {}", bill.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pausePlan(Long planId) {
        CollectionPlan plan = collectionPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "催收计划不存在");
        }
        plan.setStatus(2);
        collectionPlanMapper.updateById(plan);
        log.info("Paused collection plan: planId={}", planId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resumePlan(Long planId) {
        CollectionPlan plan = collectionPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "催收计划不存在");
        }
        plan.setStatus(1);
        collectionPlanMapper.updateById(plan);
        log.info("Resumed collection plan: planId={}", planId);
    }

    @Override
    public CollectionPlan getPlanByBillId(Long billId) {
        LambdaQueryWrapper<CollectionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollectionPlan::getBillId, billId).orderByDesc(CollectionPlan::getCreatedAt).last("LIMIT 1");
        return collectionPlanMapper.selectOne(wrapper);
    }

    @Override
    public List<CollectionLog> listLogs(Long planId) {
        LambdaQueryWrapper<CollectionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollectionLog::getPlanId, planId).orderByDesc(CollectionLog::getExecutedAt);
        return collectionLogMapper.selectList(wrapper);
    }
}
