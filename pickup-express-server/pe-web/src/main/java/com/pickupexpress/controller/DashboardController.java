package com.pickupexpress.controller;

import com.pickupexpress.common.result.R;
import com.pickupexpress.common.util.SecurityUtil;
import com.pickupexpress.domain.vo.DashboardVO;
import com.pickupexpress.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "仪表盘")
@RestController
@RequestMapping("/api/v1/evidence/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取仪表盘数据")
    @GetMapping("/")
    public R<DashboardVO> getDashboard() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            return R.fail("未获取到企业信息");
        }
        DashboardVO vo = dashboardService.getDashboard(enterpriseId);
        return R.ok(vo);
    }
}
