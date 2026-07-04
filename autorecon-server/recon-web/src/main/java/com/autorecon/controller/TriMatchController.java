package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.domain.vo.TriMatchVO;
import com.autorecon.service.TriMatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 三方匹配 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/tri-match")
@Tag(name = "三方匹配")
@RequiredArgsConstructor
public class TriMatchController {

    private final TriMatchService triMatchService;

    @GetMapping("/{billId}")
    @Operation(summary = "获取对账单三方匹配信息")
    public R<TriMatchVO> getTriMatch(@PathVariable Long billId) {
        TriMatchVO vo = triMatchService.getTriMatch(billId);
        return R.ok(vo);
    }
}
