package com.autorecon.controller;

import com.autorecon.common.result.PageResult;
import com.autorecon.common.result.R;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.LoginDTO;
import com.autorecon.domain.dto.SysUserCreateDTO;
import com.autorecon.domain.dto.SysUserUpdateDTO;
import com.autorecon.domain.entity.SysUser;
import com.autorecon.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 系统用户管理 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "系统用户管理")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @PostMapping
    @Operation(summary = "创建用户")
    public R<Long> create(@Valid @RequestBody SysUserCreateDTO dto) {
        Long id = sysUserService.createUser(dto);
        return R.ok(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SysUserUpdateDTO dto) {
        sysUserService.updateUser(id, dto);
        return R.ok();
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用用户")
    public R<Void> disable(@PathVariable Long id) {
        sysUserService.disableUser(id);
        return R.ok();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用用户")
    public R<Void> enable(@PathVariable Long id) {
        sysUserService.enableUser(id);
        return R.ok();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情")
    public R<SysUser> getUserById(@PathVariable Long id) {
        SysUser user = sysUserService.getUserById(id);
        return R.ok(user);
    }

    @GetMapping
    @Operation(summary = "用户列表")
    public R<PageResult<SysUser>> listUsers(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        PageResult<SysUser> result = sysUserService.listUsers(enterpriseId, pageNum, pageSize);
        return R.ok(result);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public R<SysUser> login(@Valid @RequestBody LoginDTO dto) {
        SysUser user = sysUserService.login(dto.getUsername(), dto.getPassword());
        return R.ok(user);
    }
}
