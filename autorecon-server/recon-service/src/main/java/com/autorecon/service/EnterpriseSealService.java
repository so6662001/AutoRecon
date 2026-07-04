package com.autorecon.service;

import com.autorecon.domain.dto.SealCreateDTO;
import com.autorecon.domain.entity.EnterpriseSeal;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 企业印章服务接口
 */
public interface EnterpriseSealService extends IService<EnterpriseSeal> {

    Long createSeal(SealCreateDTO dto);

    List<EnterpriseSeal> listSeals(Long enterpriseId);

    void disableSeal(Long sealId);

    void enableSeal(Long sealId);

    void revokeSeal(Long sealId);
}
