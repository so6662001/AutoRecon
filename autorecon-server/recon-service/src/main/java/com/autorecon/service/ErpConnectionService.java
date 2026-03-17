package com.autorecon.service;

import com.autorecon.domain.dto.ErpConnectionCreateDTO;
import com.autorecon.domain.entity.ErpConnection;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * ERP连接服务接口
 */
public interface ErpConnectionService extends IService<ErpConnection> {

    Long createConnection(ErpConnectionCreateDTO dto);

    void updateConnection(Long id, ErpConnectionCreateDTO dto);

    void deleteConnection(Long id);

    List<ErpConnection> listConnections(Long enterpriseId);

    boolean testConnection(Long id);
}
