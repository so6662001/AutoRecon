package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.domain.vo.DisputePredictionVO;
import com.autorecon.service.DisputePredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 异议预测 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/prediction")
@Tag(name = "异议预测")
@RequiredArgsConstructor
public class DisputePredictionController {

    private final DisputePredictionService disputePredictionService;

    @GetMapping("/{billId}")
    @Operation(summary = "预测对账单异议风险")
    public R<DisputePredictionVO> predict(@PathVariable Long billId) {
        DisputePredictionVO vo = disputePredictionService.predict(billId);
        return R.ok(vo);
    }
}
