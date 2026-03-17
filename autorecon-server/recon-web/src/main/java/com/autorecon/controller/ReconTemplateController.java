package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.TemplateCreateDTO;
import com.autorecon.domain.entity.ReconTemplate;
import com.autorecon.service.ReconTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对账单模板 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/templates")
@Tag(name = "对账单模板")
@RequiredArgsConstructor
public class ReconTemplateController {

    private final ReconTemplateService reconTemplateService;

    @PostMapping("/")
    @Operation(summary = "创建模板")
    public R<Long> createTemplate(@Valid @RequestBody TemplateCreateDTO dto) {
        Long id = reconTemplateService.createTemplate(dto);
        return R.ok(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模板")
    public R<Void> updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateCreateDTO dto) {
        reconTemplateService.updateTemplate(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板")
    public R<Void> deleteTemplate(@PathVariable Long id) {
        reconTemplateService.deleteTemplate(id);
        return R.ok();
    }

    @GetMapping("/")
    @Operation(summary = "模板列表")
    public R<List<ReconTemplate>> listTemplates() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        List<ReconTemplate> list = reconTemplateService.listTemplates(enterpriseId);
        return R.ok(list);
    }

    @GetMapping("/default")
    @Operation(summary = "获取默认模板")
    public R<ReconTemplate> getDefaultTemplate() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        ReconTemplate template = reconTemplateService.getDefaultTemplate(enterpriseId);
        return R.ok(template);
    }
}
