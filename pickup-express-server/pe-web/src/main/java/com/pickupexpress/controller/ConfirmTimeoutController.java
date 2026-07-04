package com.pickupexpress.controller;

import com.pickupexpress.common.result.R;
import com.pickupexpress.common.util.SecurityUtil;
import com.pickupexpress.domain.dto.ConfirmTimeoutConfigDTO;
import com.pickupexpress.domain.entity.ConfirmTimeoutConfig;
import com.pickupexpress.service.ConfirmTimeoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "确认时效配置")
@RestController
@RequestMapping("/api/v1/evidence/timeout-config")
@RequiredArgsConstructor
@Slf4j
public class ConfirmTimeoutController {

    private final ConfirmTimeoutService confirmTimeoutService;

    @Operation(summary = "获取配置")
    @GetMapping("/")
    public R<ConfirmTimeoutConfig> getConfig(
            @RequestParam Long buyerId,
            @RequestParam String scenario) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            return R.fail("未获取到企业信息");
        }
        ConfirmTimeoutConfig config = confirmTimeoutService.getConfig(enterpriseId, buyerId, scenario);
        return R.ok(config);
    }

    @Operation(summary = "保存配置")
    @PostMapping("/")
    public R<Void> saveConfig(@Valid @RequestBody ConfirmTimeoutConfigDTO dto) {
        if (dto.getEnterpriseId() == null) {
            dto.setEnterpriseId(SecurityUtil.getCurrentEnterpriseId());
        }
        confirmTimeoutService.saveConfig(dto);
        return R.ok();
    }
}
