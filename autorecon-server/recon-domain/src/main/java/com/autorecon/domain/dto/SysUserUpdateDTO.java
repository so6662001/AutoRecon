package com.autorecon.domain.dto;

import lombok.Data;

/**
 * 系统用户更新 DTO
 */
@Data
public class SysUserUpdateDTO {

    private String realName;
    private String phone;
    private String email;
    private Integer roleType;
    private String avatarUrl;
}
