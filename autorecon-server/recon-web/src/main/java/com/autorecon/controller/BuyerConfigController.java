package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.BuyerDataConfigDTO;
import com.autorecon.domain.entity.BuyerDataConfig;
import com.autorecon.service.BuyerConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 买方数据配置 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/buyer/config")
@Tag(name = "买方数据配置")
@RequiredArgsConstructor
public class BuyerConfigController {

    private final BuyerConfigService buyerConfigService;

    @GetMapping
    @Operation(summary = "获取买方配置")
    public R<BuyerDataConfig> getConfig() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        BuyerDataConfig config = buyerConfigService.getConfig(enterpriseId);
        return R.ok(config);
    }

    @PutMapping
    @Operation(summary = "保存买方配置")
    public R<Void> saveConfig(@Valid @RequestBody BuyerDataConfigDTO dto) {
        buyerConfigService.saveConfig(dto);
        return R.ok();
    }
}
