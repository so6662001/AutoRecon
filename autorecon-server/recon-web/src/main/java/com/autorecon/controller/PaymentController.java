package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.domain.dto.PaymentAllocateDTO;
import com.autorecon.domain.dto.PaymentCreateDTO;
import com.autorecon.domain.entity.Payment;
import com.autorecon.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 付款管理 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/payments")
@Tag(name = "付款管理")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/")
    @Operation(summary = "创建付款")
    public R<Long> createPayment(@Valid @RequestBody PaymentCreateDTO dto) {
        Long id = paymentService.createPayment(dto);
        return R.ok(id);
    }

    @PostMapping("/{id}/allocate")
    @Operation(summary = "分配付款")
    public R<Void> allocatePayment(@PathVariable Long id, @Valid @RequestBody PaymentAllocateDTO dto) {
        dto.setPaymentId(id);
        paymentService.allocatePayment(dto);
        return R.ok();
    }

    @PostMapping("/{id}/auto-allocate")
    @Operation(summary = "自动分配付款(FIFO)")
    public R<Void> autoAllocateFIFO(@PathVariable Long id) {
        paymentService.autoAllocateFIFO(id);
        return R.ok();
    }

    @GetMapping("/balance")
    @Operation(summary = "获取余额")
    public R<BigDecimal> getBalance(@RequestParam Long sellerId, @RequestParam Long buyerId) {
        BigDecimal balance = paymentService.getBalance(sellerId, buyerId);
        return R.ok(balance);
    }

    @GetMapping("/")
    @Operation(summary = "付款列表")
    public R<List<Payment>> listPayments(
            @RequestParam Long payerId,
            @RequestParam Long payeeId,
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end) {
        List<Payment> list = paymentService.listPayments(payerId, payeeId, start, end);
        return R.ok(list);
    }
}
