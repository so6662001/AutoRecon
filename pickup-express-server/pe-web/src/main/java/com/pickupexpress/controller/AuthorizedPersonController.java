package com.pickupexpress.controller;

import com.pickupexpress.common.result.R;
import com.pickupexpress.domain.dto.PickupPersonRegisterDTO;
import com.pickupexpress.domain.entity.AuthorizedPickupPerson;
import com.pickupexpress.service.AuthorizedPickupPersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "授权提货人")
@RestController
@RequestMapping("/api/v1/evidence/authorized-persons")
@RequiredArgsConstructor
@Slf4j
public class AuthorizedPersonController {

    private final AuthorizedPickupPersonService authorizedPickupPersonService;

    @Operation(summary = "注册授权提货人")
    @PostMapping("/")
    public R<Long> register(@Valid @RequestBody PickupPersonRegisterDTO dto) {
        Long id = authorizedPickupPersonService.register(dto);
        return R.ok(id);
    }

    @Operation(summary = "按买方查询授权提货人列表")
    @GetMapping("/")
    public R<List<AuthorizedPickupPerson>> listByBuyer(@RequestParam Long buyerId) {
        List<AuthorizedPickupPerson> list = authorizedPickupPersonService.listByBuyer(buyerId);
        return R.ok(list);
    }

    @Operation(summary = "买方确认")
    @PutMapping("/{id}/confirm")
    public R<Void> confirmByBuyer(@PathVariable Long id) {
        authorizedPickupPersonService.confirmByBuyer(id);
        return R.ok();
    }

    @Operation(summary = "停用授权提货人")
    @PutMapping("/{id}/disable")
    public R<Void> disable(@PathVariable Long id) {
        authorizedPickupPersonService.disable(id);
        return R.ok();
    }
}
