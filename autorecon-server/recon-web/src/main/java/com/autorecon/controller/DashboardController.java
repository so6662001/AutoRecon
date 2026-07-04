package com.autorecon.controller;

import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.vo.DashboardVO;
import com.autorecon.service.ReconBillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作台 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon/dashboard")
@Tag(name = "工作台")
@RequiredArgsConstructor
public class DashboardController {

    private final ReconBillService reconBillService;

    @GetMapping("/")
    @Operation(summary = "获取工作台数据")
    public R<DashboardVO> getDashboard() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        DashboardVO vo = reconBillService.getDashboard(enterpriseId);
        return R.ok(vo);
    }
}
