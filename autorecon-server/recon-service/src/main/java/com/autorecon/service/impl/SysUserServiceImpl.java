package com.autorecon.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.result.PageResult;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.SysUserCreateDTO;
import com.autorecon.domain.dto.SysUserUpdateDTO;
import com.autorecon.domain.entity.SysUser;
import com.autorecon.mapper.SysUserMapper;
import com.autorecon.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 系统用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(SysUserCreateDTO dto) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, dto.getUsername());
        if (dto.getEnterpriseId() != null) {
            wrapper.eq(SysUser::getEnterpriseId, dto.getEnterpriseId());
        }
        if (sysUserMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "用户名已存在");
        }

        String hashedPassword = DigestUtil.md5Hex(dto.getPassword());
        SysUser user = SysUser.builder()
                .enterpriseId(dto.getEnterpriseId())
                .username(dto.getUsername())
                .password(hashedPassword)
                .realName(dto.getRealName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .roleType(dto.getRoleType() != null ? dto.getRoleType() : 1)
                .status(1)
                .build();
        sysUserMapper.insert(user);
        log.info("Created user: id={}, username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, SysUserUpdateDTO dto) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "用户不存在");
        }
        if (dto.getRealName() != null) user.setRealName(dto.getRealName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getRoleType() != null) user.setRoleType(dto.getRoleType());
        if (dto.getAvatarUrl() != null) user.setAvatarUrl(dto.getAvatarUrl());
        sysUserMapper.updateById(user);
        log.info("Updated user: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "用户不存在");
        }
        user.setStatus(0);
        sysUserMapper.updateById(user);
        log.info("Disabled user: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "用户不存在");
        }
        user.setStatus(1);
        sysUserMapper.updateById(user);
        log.info("Enabled user: id={}", id);
    }

    @Override
    public SysUser getUserById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public PageResult<SysUser> listUsers(Long enterpriseId, Integer pageNum, Integer pageSize) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getEnterpriseId, eid).orderByDesc(SysUser::getCreatedAt);

        int pn = pageNum != null && pageNum > 0 ? pageNum : 1;
        int ps = pageSize != null && pageSize > 0 ? pageSize : 10;
        Page<SysUser> page = new Page<>(pn, ps);
        IPage<SysUser> result = sysUserMapper.selectPage(page, wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser login(String username, String password) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username).last("LIMIT 1");
        SysUser user = sysUserMapper.selectOne(wrapper);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        String hashedPassword = DigestUtil.md5Hex(password);
        if (!hashedPassword.equals(user.getPassword())) {
            throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "用户已禁用");
        }
        user.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        user.setPassword(null);
        log.info("User logged in: id={}, username={}", user.getId(), username);
        return user;
    }
}
