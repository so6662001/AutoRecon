package com.pickupexpress.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pickupexpress.domain.dto.ContractQueryDTO;
import com.pickupexpress.domain.entity.Contract;
import com.pickupexpress.domain.entity.ContractItem;
import com.pickupexpress.domain.vo.ContractDetailVO;
import com.pickupexpress.domain.vo.ContractVO;
import com.pickupexpress.common.result.PageResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合同服务接口
 */
public interface ContractService extends IService<Contract> {

    void syncFromErp(Contract contract, List<ContractItem> items);

    PageResult<ContractVO> queryContracts(ContractQueryDTO query);

    ContractDetailVO getContractDetail(Long contractId);

    void initiateSign(Long contractId);

    void customerSign(Long contractId);

    void updatePickedAmount(Long contractId, BigDecimal weight, BigDecimal amount);
}
