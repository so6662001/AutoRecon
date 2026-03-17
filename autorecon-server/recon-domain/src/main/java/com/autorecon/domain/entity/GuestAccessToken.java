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
 * 免注册访问令牌
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("guest_access_token")
public class GuestAccessToken {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String token;
    private Long billId;
    private String buyerPhoneHash;
    private LocalDateTime expireAt;
    private Integer phoneVerified;
    private Integer openedCount;
    private LocalDateTime firstOpenedAt;
    private Integer confirmed;
    private LocalDateTime confirmedAt;
    private String disputeMessage;
    private String ipAddress;
    private String userAgent;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
