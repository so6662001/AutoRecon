package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.AuthorizationChangeRespondDTO;
import com.autorecon.domain.dto.AuthorizationItemDTO;
import com.autorecon.domain.dto.EnterpriseAuthorizationDTO;
import com.autorecon.domain.vo.AuthorizationCheckResult;
import com.autorecon.domain.vo.EnterpriseAuthorizationVO;
import com.autorecon.service.DataAuthorizationService;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业数据授权 API
 */
@RestController
@RequestMapping("/api/v1/data-auth")
@Tag(name = "企业数据授权")
@RequiredArgsConstructor
public class DataAuthorizationController {

    private final DataAuthorizationService dataAuthorizationService;

    @GetMapping
    @Operation(summary = "获取当前企业数据授权状态")
    public R<EnterpriseAuthorizationVO> getAuthorizationStatus() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        return R.ok(dataAuthorizationService.getAuthorizationStatus(enterpriseId));
    }

    @PostMapping("/initialize")
    @Operation(summary = "首次初始化企业数据授权（全部为已授权）")
    public R<Void> initialize(HttpServletRequest request) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        Long userId = SecurityUtil.getCurrentUserId();
        dataAuthorizationService.initializeForEnterprise(enterpriseId, userId,
                request.getRemoteAddr(), request.getHeader("User-Agent"));
        return R.ok();
    }

    @PutMapping("/update")
    @Operation(summary = "更新企业数据授权（管理员）")
    public R<Void> update(@Valid @RequestBody EnterpriseAuthorizationDTO dto, HttpServletRequest request) {
        dataAuthorizationService.updateAuthorizations(dto.getEnterpriseId(), dto.getAuthorizations(),
                request.getRemoteAddr(), request.getHeader("User-Agent"));
        return R.ok();
    }

    @PutMapping("/revoke")
    @Operation(summary = "撤回指定类型的数据授权")
    public R<Void> revoke(@RequestParam String type) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        dataAuthorizationService.revokeAuthorization(enterpriseId, type);
        return R.ok();
    }

    @GetMapping("/check")
    @Operation(summary = "授权检查结果（数据服务/内部使用）")
    public R<AuthorizationCheckResult> check() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        return R.ok(dataAuthorizationService.checkAuthorization(enterpriseId));
    }

    @GetMapping("/changes")
    @Operation(summary = "待处理的授权变更通知")
    public R<List<EnterpriseAuthorizationVO.ChangeNotification>> listPendingChanges() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        EnterpriseAuthorizationVO vo = dataAuthorizationService.getAuthorizationStatus(enterpriseId);
        return R.ok(vo.getPendingChanges());
    }

    @PutMapping("/changes/{id}/respond")
    @Operation(summary = "响应授权变更通知")
    public R<Void> respond(@PathVariable Long id, @Valid @RequestBody AuthorizationChangeRespondDTO dto) {
        dataAuthorizationService.respondToChange(id, Boolean.TRUE.equals(dto.getAccept()), dto.getDetail());
        return R.ok();
    }
}
