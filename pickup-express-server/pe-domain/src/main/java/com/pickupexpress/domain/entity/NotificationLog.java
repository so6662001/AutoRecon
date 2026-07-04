package com.pickupexpress.domain.entity;

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
 * 通知日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("notification_log")
public class NotificationLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String targetType;
    private Long targetId;
    @TableField("recipient_phone")
    private String recipient;
    private Integer channel;
    private String title;
    private String content;
    private Integer sent;
    private LocalDateTime sentAt;
    private String failReason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
