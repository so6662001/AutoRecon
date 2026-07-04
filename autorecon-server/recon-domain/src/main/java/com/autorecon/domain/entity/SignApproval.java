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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 签章审批
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sign_approval")
public class SignApproval {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long signRecordId;
    private Long billId;
    private Long applicantId;
    private Long approverId;
    private Long sealId;
    private BigDecimal billAmount;
    private Integer approvalStatus;
    private String approvalComment;
    private Integer approvalChannel;
    private LocalDateTime appliedAt;
    private LocalDateTime approvedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
