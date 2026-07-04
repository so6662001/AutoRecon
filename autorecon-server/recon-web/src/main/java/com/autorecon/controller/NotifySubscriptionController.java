package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.NotifySubscriptionDTO;
import com.autorecon.domain.entity.NotifySubscription;
import com.autorecon.service.NotifySubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知订阅 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/subscriptions")
@Tag(name = "通知订阅")
@RequiredArgsConstructor
public class NotifySubscriptionController {

    private final NotifySubscriptionService notifySubscriptionService;

    @GetMapping("/")
    @Operation(summary = "获取当前用户订阅配置")
    public R<List<NotifySubscription>> getSubscriptions() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<NotifySubscription> list = notifySubscriptionService.getSubscriptions(userId);
        return R.ok(list);
    }

    @PutMapping("/")
    @Operation(summary = "更新订阅配置")
    public R<Void> updateSubscriptions(@Valid @RequestBody List<NotifySubscriptionDTO> subscriptions) {
        Long userId = SecurityUtil.getCurrentUserId();
        notifySubscriptionService.updateSubscriptions(userId, subscriptions);
        return R.ok();
    }
}
