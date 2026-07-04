package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.ErpConnectionCreateDTO;
import com.autorecon.domain.entity.ErpConnection;
import com.autorecon.service.ErpConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ERP连接管理 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/erp/connections")
@Tag(name = "ERP连接管理")
@RequiredArgsConstructor
public class ErpConnectionController {

    private final ErpConnectionService erpConnectionService;

    @PostMapping
    @Operation(summary = "创建ERP连接")
    public R<Long> create(@Valid @RequestBody ErpConnectionCreateDTO dto) {
        Long id = erpConnectionService.createConnection(dto);
        return R.ok(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新ERP连接")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ErpConnectionCreateDTO dto) {
        erpConnectionService.updateConnection(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除ERP连接")
    public R<Void> delete(@PathVariable Long id) {
        erpConnectionService.deleteConnection(id);
        return R.ok();
    }

    @GetMapping
    @Operation(summary = "ERP连接列表")
    public R<List<ErpConnection>> list() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        List<ErpConnection> list = erpConnectionService.listConnections(enterpriseId);
        return R.ok(list);
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试ERP连接")
    public R<Boolean> test(@PathVariable Long id) {
        boolean result = erpConnectionService.testConnection(id);
        return R.ok(result);
    }
}
