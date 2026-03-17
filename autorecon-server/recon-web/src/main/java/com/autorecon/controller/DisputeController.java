package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.domain.dto.DisputeCreateDTO;
import com.autorecon.domain.dto.DisputeMessageDTO;
import com.autorecon.domain.entity.Dispute;
import com.autorecon.domain.entity.DisputeMessage;
import com.autorecon.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 异议管理 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/disputes")
@Tag(name = "异议管理")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping("/")
    @Operation(summary = "创建异议")
    public R<Long> createDispute(@Valid @RequestBody DisputeCreateDTO dto) {
        Long id = disputeService.createDispute(dto);
        return R.ok(id);
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "发送异议消息")
    public R<Void> sendMessage(@PathVariable Long id, @RequestBody DisputeMessageDTO dto) {
        dto.setDisputeId(id);
        disputeService.sendMessage(dto);
        return R.ok();
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "解决异议")
    public R<Void> resolveDispute(@PathVariable Long id, @RequestParam String resolution) {
        disputeService.resolveDispute(id, resolution);
        return R.ok();
    }

    @GetMapping("/")
    @Operation(summary = "异议列表")
    public R<List<Dispute>> listDisputes(@RequestParam Long billId) {
        List<Dispute> list = disputeService.listDisputes(billId);
        return R.ok(list);
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "获取异议消息列表")
    public R<List<DisputeMessage>> getMessages(@PathVariable Long id) {
        List<DisputeMessage> list = disputeService.getMessages(id);
        return R.ok(list);
    }
}
