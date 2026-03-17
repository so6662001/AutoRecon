package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.domain.dto.GuestConfirmDTO;
import com.autorecon.domain.dto.GuestTokenGenerateDTO;
import com.autorecon.domain.vo.GuestBillVO;
import com.autorecon.service.GuestAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 免注册访问 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/guest")
@Tag(name = "免注册访问")
@RequiredArgsConstructor
public class GuestAccessController {

    private final GuestAccessService guestAccessService;

    @GetMapping("/view/{token}")
    @Operation(summary = "通过令牌查看对账单")
    public R<GuestBillVO> viewBill(@PathVariable String token) {
        GuestBillVO vo = guestAccessService.viewBill(token);
        return R.ok(vo);
    }

    @PostMapping("/verify-phone")
    @Operation(summary = "验证手机号")
    public R<Boolean> verifyPhone(@RequestParam String token, @RequestParam String code) {
        boolean result = guestAccessService.verifyPhone(token, code);
        return R.ok(result);
    }

    @PostMapping("/confirm/{token}")
    @Operation(summary = "访客确认")
    public R<Void> guestConfirm(@PathVariable String token, @RequestBody GuestConfirmDTO dto) {
        dto.setToken(token);
        guestAccessService.guestConfirm(dto);
        return R.ok();
    }

    @PostMapping("/generate")
    @Operation(summary = "生成访客令牌（需认证，仅对账单卖方可操作）")
    public R<String> generateGuestToken(@Valid @RequestBody GuestTokenGenerateDTO dto) {
        String token = guestAccessService.generateGuestToken(dto.getBillId(), dto.getBuyerPhone());
        return R.ok(token);
    }
}
