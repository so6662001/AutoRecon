package com.autorecon.controller;

import com.autorecon.common.result.PageResult;
import com.autorecon.common.result.R;
import com.autorecon.domain.dto.EnterpriseAuthDTO;
import com.autorecon.domain.dto.EnterpriseCreateDTO;
import com.autorecon.domain.entity.Enterprise;
import com.autorecon.domain.entity.EnterpriseAuth;
import com.autorecon.service.EnterpriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 企业管理 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/enterprises")
@Tag(name = "企业管理")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    @PostMapping
    @Operation(summary = "创建企业")
    public R<Long> create(@Valid @RequestBody EnterpriseCreateDTO dto) {
        Long id = enterpriseService.createEnterprise(dto);
        return R.ok(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新企业")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody EnterpriseCreateDTO dto) {
        enterpriseService.updateEnterprise(id, dto);
        return R.ok();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取企业详情")
    public R<Enterprise> get(@PathVariable Long id) {
        Enterprise enterprise = enterpriseService.getEnterprise(id);
        return R.ok(enterprise);
    }

    @GetMapping
    @Operation(summary = "企业列表")
    public R<PageResult<Enterprise>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        PageResult<Enterprise> result = enterpriseService.listEnterprises(keyword, type, pageNum, pageSize);
        return R.ok(result);
    }

    @PostMapping("/{id}/auth")
    @Operation(summary = "提交企业认证")
    public R<Void> submitAuth(@PathVariable Long id, @Valid @RequestBody EnterpriseAuthDTO dto) {
        enterpriseService.submitAuth(id, dto);
        return R.ok();
    }

    @GetMapping("/{id}/auth")
    @Operation(summary = "获取认证状态")
    public R<EnterpriseAuth> getAuthStatus(@PathVariable Long id) {
        EnterpriseAuth auth = enterpriseService.getAuthStatus(id);
        return R.ok(auth);
    }
}
