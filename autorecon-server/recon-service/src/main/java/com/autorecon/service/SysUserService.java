package com.autorecon.service;

import com.autorecon.common.result.PageResult;
import com.autorecon.domain.dto.SysUserCreateDTO;
import com.autorecon.domain.dto.SysUserUpdateDTO;
import com.autorecon.domain.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 系统用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    Long createUser(SysUserCreateDTO dto);

    void updateUser(Long id, SysUserUpdateDTO dto);

    void disableUser(Long id);

    void enableUser(Long id);

    SysUser getUserById(Long id);

    PageResult<SysUser> listUsers(Long enterpriseId, Integer pageNum, Integer pageSize);

    SysUser login(String username, String password);
}
