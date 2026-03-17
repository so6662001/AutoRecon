package com.pickupexpress.controller;

import com.pickupexpress.common.result.R;
import com.pickupexpress.common.util.SecurityUtil;
import com.pickupexpress.domain.dto.CarrierCreateDTO;
import com.pickupexpress.domain.entity.Carrier;
import com.pickupexpress.service.CarrierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "承运公司管理")
@RestController
@RequestMapping("/api/v1/evidence/carrier")
@RequiredArgsConstructor
@Slf4j
public class CarrierController {

    private final CarrierService carrierService;

    @Operation(summary = "创建承运公司")
    @PostMapping("/")
    public R<Long> createCarrier(@Valid @RequestBody CarrierCreateDTO dto) {
        Long id = carrierService.createCarrier(dto.getName(), dto.getContactName(), dto.getContactPhone());
        return R.ok(id);
    }

    @Operation(summary = "承运公司列表")
    @GetMapping("/")
    public R<List<Carrier>> listCarriers() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            return R.fail("未获取到企业信息");
        }
        List<Carrier> list = carrierService.listCarriers(enterpriseId);
        return R.ok(list);
    }
}
