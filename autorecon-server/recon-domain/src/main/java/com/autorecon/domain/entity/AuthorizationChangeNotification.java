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
 * 授权变更通知
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("authorization_change_notification")
public class AuthorizationChangeNotification {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String changeType;
    private String changeSummary;
    private String newAuthorizationTypes;
    private Long notifiedUserId;
    private LocalDateTime notifiedAt;
    /** 0-待处理 1-已确认 2-已拒绝 3-超时 */
    private Integer responseStatus;
    private LocalDateTime respondedAt;
    private String responseDetail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
