package com.pickupexpress.controller;

import com.pickupexpress.common.result.R;
import com.pickupexpress.common.util.SecurityUtil;
import com.pickupexpress.domain.dto.WarehouseCreateDTO;
import com.pickupexpress.domain.entity.Warehouse;
import com.pickupexpress.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "仓库管理")
@RestController
@RequestMapping("/api/v1/evidence/warehouse")
@RequiredArgsConstructor
@Slf4j
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "创建仓库")
    @PostMapping("/")
    public R<Long> createWarehouse(@Valid @RequestBody WarehouseCreateDTO dto) {
        Long id = warehouseService.createWarehouse(dto);
        return R.ok(id);
    }

    @Operation(summary = "更新仓库")
    @PutMapping("/{id}")
    public R<Void> updateWarehouse(@PathVariable Long id, @Valid @RequestBody WarehouseCreateDTO dto) {
        warehouseService.updateWarehouse(id, dto);
        return R.ok();
    }

    @Operation(summary = "仓库列表")
    @GetMapping("/")
    public R<List<Warehouse>> listWarehouses() {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            return R.fail("未获取到企业信息");
        }
        List<Warehouse> list = warehouseService.listWarehouses(enterpriseId);
        return R.ok(list);
    }
}
