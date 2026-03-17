package com.autorecon.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 系统用户创建 DTO
 */
@Data
public class SysUserCreateDTO {

    private Long enterpriseId;
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    private String realName;
    private String phone;
    private String email;
    private Integer roleType;
}
