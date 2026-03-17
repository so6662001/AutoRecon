package com.autorecon.service;

import com.autorecon.domain.dto.SealOperatorCreateDTO;
import com.autorecon.domain.dto.SealOperatorUpdateDTO;
import com.autorecon.domain.entity.SealOperator;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 签章经办人服务接口
 */
public interface SealOperatorService extends IService<SealOperator> {

    Long addOperator(SealOperatorCreateDTO dto);

    List<SealOperator> listOperators(Long enterpriseId);

    void updateOperator(Long id, SealOperatorUpdateDTO dto);

    void disableOperator(Long id);
}
