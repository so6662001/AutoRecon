package com.autorecon.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统用户
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long enterpriseId;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private Integer roleType;
    private String avatarUrl;
    private Integer status;
    private LocalDateTime lastLoginAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
