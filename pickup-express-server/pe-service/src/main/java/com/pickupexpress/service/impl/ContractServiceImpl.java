package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickupexpress.common.exception.BizException;
import com.pickupexpress.common.exception.ErrorCode;
import com.pickupexpress.common.result.PageResult;
import com.pickupexpress.common.util.TenantUtil;
import com.pickupexpress.domain.dto.ContractQueryDTO;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.ContractItem;
import com.pickupexpress.domain.entity.PickupOrder;
import com.pickupexpress.domain.entity.ProgressEvent;
import com.pickupexpress.domain.entity.SettlementOrder;
import com.pickupexpress.domain.enums.ContractStatusEnum;
import com.pickupexpress.domain.enums.ContractTypeEnum;
import com.pickupexpress.domain.enums.SignStatusEnum;
import com.pickupexpress.domain.vo.ContractDetailVO;
import com.pickupexpress.domain.vo.ContractVO;
import com.pickupexpress.domain.vo.PickupOrderVO;
import com.pickupexpress.mapper.ContractItemMapper;
import com.pickupexpress.mapper.ContractMapper;
import com.pickupexpress.mapper.PickupOrderMapper;
import com.pickupexpress.mapper.ProgressEventMapper;
import com.pickupexpress.mapper.SettlementOrderMapper;
import com.pickupexpress.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 合同服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements ContractService {

    private final ContractItemMapper contractItemMapper;
    private final PickupOrderMapper pickupOrderMapper;
    private final SettlementOrderMapper settlementOrderMapper;
    private final ProgressEventMapper progressEventMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromErp(Contract contract, List<ContractItem> items) {
        save(contract);
        if (items != null && !items.isEmpty()) {
            for (ContractItem item : items) {
                item.setContractId(contract.getId());
                contractItemMapper.insert(item);
            }
        }
        ContractTypeEnum typeEnum = ContractTypeEnum.of(contract.getContractType());
        if (typeEnum == ContractTypeEnum.RESERVED) {
            contract.setStatus(ContractStatusEnum.READY.getValue());
        } else if (typeEnum == ContractTypeEnum.ORDER) {
            contract.setStatus(ContractStatusEnum.PENDING_SIGN.getValue());
        }
        updateById(contract);
    }

    @Override
    public PageResult<ContractVO> queryContracts(ContractQueryDTO query) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        Long enterpriseId = com.pickupexpress.common.util.SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId != null) {
            wrapper.and(w -> w.eq(Contract::getSellerId, enterpriseId).or().eq(Contract::getBuyerId, enterpriseId));
        }
        if (query.getSellerId() != null) {
            wrapper.eq(Contract::getSellerId, query.getSellerId());
        }
        if (query.getBuyerId() != null) {
            wrapper.eq(Contract::getBuyerId, query.getBuyerId());
        }
        if (StringUtils.hasText(query.getContractNo())) {
            wrapper.like(Contract::getContractNo, query.getContractNo());
        }
        if (query.getContractType() != null) {
            wrapper.eq(Contract::getContractType, query.getContractType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Contract::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Contract::getContractNo, query.getKeyword())
                    .or().like(Contract::getErpContractId, query.getKeyword()));
        }
        wrapper.orderByDesc(Contract::getCreatedAt);

        int pageNum = query.getPageNum() != null && query.getPageNum() > 0 ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 20;
        IPage<Contract> page = page(new Page<>(pageNum, pageSize), wrapper);

        List<ContractVO> voList = page.getRecords().stream().map(c -> {
            ContractVO vo = new ContractVO();
            BeanUtils.copyProperties(c, vo);
            vo.setItemCount(contractItemMapper.selectCount(
                    new LambdaQueryWrapper<ContractItem>().eq(ContractItem::getContractId, c.getId())).intValue());
            vo.setPickupCount(pickupOrderMapper.selectCount(
                    new LambdaQueryWrapper<com.pickupexpress.domain.entity.PickupOrder>().eq(com.pickupexpress.domain.entity.PickupOrder::getContractId, c.getId())).intValue());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, page.getTotal(), page.getSize(), page.getCurrent(), page.getPages());
    }

    @Override
    public ContractDetailVO getContractDetail(Long contractId) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw new BizException(ErrorCode.CONTRACT_NOT_FOUND);
        }
        TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());

        ContractDetailVO vo = new ContractDetailVO();
        BeanUtils.copyProperties(contract, vo);
        vo.setItems(contractItemMapper.selectList(new LambdaQueryWrapper<ContractItem>().eq(ContractItem::getContractId, contractId)));

        String erpBuyerName = parseErpBuyerName(contract);
        List<PickupOrder> orders = pickupOrderMapper.selectList(
                new LambdaQueryWrapper<PickupOrder>().eq(PickupOrder::getContractId, contractId).orderByDesc(PickupOrder::getCreatedAt));
        List<PickupOrderVO> pickupVos = orders.stream().map(o -> {
            PickupOrderVO pvo = new PickupOrderVO();
            BeanUtils.copyProperties(o, pvo);
            pvo.setContractNo(contract.getContractNo());
            pvo.setBuyerName(erpBuyerName);
            return pvo;
        }).collect(Collectors.toList());
        vo.setPickupOrders(pickupVos);

        vo.setSettlements(settlementOrderMapper.selectList(
                new LambdaQueryWrapper<SettlementOrder>().eq(SettlementOrder::getContractId, contractId).orderByDesc(SettlementOrder::getCreatedAt)));
        vo.setProgressEvents(progressEventMapper.selectList(
                new LambdaQueryWrapper<ProgressEvent>().eq(ProgressEvent::getContractId, contractId).orderByDesc(ProgressEvent::getCreatedAt)));
        return vo;
    }

    private static String parseErpBuyerName(Contract contract) {
        if (contract == null || contract.getCustomClauses() == null) {
            return null;
        }
        String prefix = "erpBuyerName=";
        String clauses = contract.getCustomClauses();
        if (clauses.startsWith(prefix)) {
            return clauses.substring(prefix.length());
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initiateSign(Long contractId) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw new BizException(ErrorCode.CONTRACT_NOT_FOUND);
        }
        TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        contract.setSignStatus(SignStatusEnum.NOT_SIGNED.getValue());
        updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void customerSign(Long contractId) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw new BizException(ErrorCode.CONTRACT_NOT_FOUND);
        }
        TenantUtil.checkContractAccess(contract.getSellerId(), contract.getBuyerId());
        contract.setSignStatus(SignStatusEnum.BUYER_SIGNED.getValue());
        updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePickedAmount(Long contractId, BigDecimal weight, BigDecimal amount) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw new BizException(ErrorCode.CONTRACT_NOT_FOUND);
        }
        BigDecimal newWeight = (contract.getPickedWeight() != null ? contract.getPickedWeight() : BigDecimal.ZERO).add(weight != null ? weight : BigDecimal.ZERO);
        BigDecimal newAmount = (contract.getPickedAmount() != null ? contract.getPickedAmount() : BigDecimal.ZERO).add(amount != null ? amount : BigDecimal.ZERO);
        contract.setPickedWeight(newWeight);
        contract.setPickedAmount(newAmount);
        updateById(contract);
    }
}
