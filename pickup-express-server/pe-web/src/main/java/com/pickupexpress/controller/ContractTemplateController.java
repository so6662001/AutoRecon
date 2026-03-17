package com.pickupexpress.controller;

import com.pickupexpress.common.result.R;
import com.pickupexpress.common.util.SecurityUtil;
import com.pickupexpress.domain.dto.ContractTemplateCreateDTO;
import com.pickupexpress.domain.entity.ContractTemplate;
import com.pickupexpress.service.ContractTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "合同模板管理")
@RestController
@RequestMapping("/api/v1/evidence/templates")
@RequiredArgsConstructor
@Slf4j
public class ContractTemplateController {

    private final ContractTemplateService contractTemplateService;

    @Operation(summary = "创建模板")
    @PostMapping("/")
    public R<Long> createTemplate(@Valid @RequestBody ContractTemplateCreateDTO dto) {
        Long id = contractTemplateService.createTemplate(dto);
        return R.ok(id);
    }

    @Operation(summary = "更新模板")
    @PutMapping("/{id}")
    public R<Void> updateTemplate(@PathVariable Long id, @Valid @RequestBody ContractTemplateCreateDTO dto) {
        contractTemplateService.updateTemplate(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    public R<Void> deleteTemplate(@PathVariable Long id) {
        contractTemplateService.deleteTemplate(id);
        return R.ok();
    }

    @Operation(summary = "模板列表")
    @GetMapping("/")
    public R<List<ContractTemplate>> listTemplates(@RequestParam(required = false) Integer contractType) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            return R.fail("未获取到企业信息");
        }
        List<ContractTemplate> list = contractTemplateService.listTemplates(enterpriseId, contractType);
        return R.ok(list);
    }

    @Operation(summary = "获取默认模板")
    @GetMapping("/default")
    public R<ContractTemplate> getDefaultTemplate(@RequestParam Integer contractType) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            return R.fail("未获取到企业信息");
        }
        ContractTemplate template = contractTemplateService.getDefaultTemplate(enterpriseId, contractType);
        return R.ok(template);
    }
}
