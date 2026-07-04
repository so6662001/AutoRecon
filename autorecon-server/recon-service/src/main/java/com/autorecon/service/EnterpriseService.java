package com.autorecon.service;

import com.autorecon.common.result.PageResult;
import com.autorecon.domain.dto.EnterpriseAuthDTO;
import com.autorecon.domain.dto.EnterpriseCreateDTO;
import com.autorecon.domain.entity.Enterprise;
import com.autorecon.domain.entity.EnterpriseAuth;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 企业服务接口
 */
public interface EnterpriseService extends IService<Enterprise> {

    Long createEnterprise(EnterpriseCreateDTO dto);

    void updateEnterprise(Long id, EnterpriseCreateDTO dto);

    Enterprise getEnterprise(Long id);

    PageResult<Enterprise> listEnterprises(String keyword, Integer type, Integer pageNum, Integer pageSize);

    void submitAuth(Long enterpriseId, EnterpriseAuthDTO dto);

    EnterpriseAuth getAuthStatus(Long enterpriseId);
}
