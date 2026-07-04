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
 * 进度事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("progress_event")
public class ProgressEvent {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long pickupOrderId;
    private Long contractId;
    private String eventType;
    private String eventTitle;
    private String eventDetail;
    private String operator;
    private String notifyTargets;
    private Integer notified;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
