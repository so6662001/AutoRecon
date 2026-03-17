package com.autorecon.service;

import com.autorecon.domain.dto.DisputeCreateDTO;
import com.autorecon.domain.dto.DisputeMessageDTO;
import com.autorecon.domain.entity.Dispute;
import com.autorecon.domain.entity.DisputeMessage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 异议服务接口
 */
public interface DisputeService extends IService<Dispute> {

    Long createDispute(DisputeCreateDTO dto);

    void sendMessage(DisputeMessageDTO dto);

    void resolveDispute(Long disputeId, String resolution);

    List<Dispute> listDisputes(Long billId);

    List<DisputeMessage> getMessages(Long disputeId);
}
