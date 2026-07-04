package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.common.util.TenantUtil;
import com.autorecon.domain.dto.TemplateCreateDTO;
import com.autorecon.domain.entity.ReconTemplate;
import com.autorecon.domain.vo.TemplatePreviewVO;
import com.autorecon.service.ReconTemplateService;
import com.autorecon.service.impl.TemplateRenderService;
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

    private final ReconTemplateService templateService;
    private final TemplateRenderService templateRenderService;

    @PostMapping("/")
    @Operation(summary = "创建模板")
    public R<Long> createTemplate(@Valid @RequestBody TemplateCreateDTO dto) {
        Long id = templateService.createTemplate(dto);
        return R.ok(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模板")
    public R<Void> updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateCreateDTO dto) {
        templateService.updateTemplate(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板")
    public R<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return R.ok();
    }

    @GetMapping("/")
    @Operation(summary = "模板列表")
    public R<List<ReconTemplate>> listTemplates() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        List<ReconTemplate> list = templateService.listTemplates(enterpriseId);
        return R.ok(list);
    }

    @GetMapping("/default")
    @Operation(summary = "获取默认模板")
    public R<ReconTemplate> getDefaultTemplate() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        ReconTemplate template = templateService.getDefaultTemplate(enterpriseId);
        return R.ok(template);
    }

    @Operation(summary = "模板详情")
    @GetMapping("/{id}")
    public R<ReconTemplate> getTemplate(@PathVariable Long id) {
        ReconTemplate t = templateService.getById(id);
        if (t == null) {
            return R.fail("模板不存在");
        }
        TenantUtil.checkOwnership(t.getEnterpriseId());
        return R.ok(t);
    }

    @Operation(summary = "复制模板")
    @PostMapping("/{id}/copy")
    public R<Long> copyTemplate(@PathVariable Long id) {
        Long newId = templateService.copyTemplate(id);
        return R.ok(newId);
    }

    @Operation(summary = "模板预览(使用示例数据)")
    @GetMapping("/{id}/preview")
    public R<TemplatePreviewVO> previewTemplate(@PathVariable Long id) {
        TemplatePreviewVO preview = templateRenderService.preview(id);
        if (preview == null) {
            return R.fail("模板不存在");
        }
        return R.ok(preview);
    }
}
