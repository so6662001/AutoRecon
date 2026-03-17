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
 * 签章记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sign_record")
public class SignRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long billId;
    private String signFlowId;
    private Integer signOrderType;
    private LocalDateTime signDeadline;
    private Integer sellerSignStatus;
    private Long sellerSealId;
    private Long sellerOperatorId;
    private Integer sellerVerifyMethod;
    private LocalDateTime sellerSignAt;
    private String sellerSignIp;
    private String sellerSignDevice;
    private Integer sellerSignChannel;
    private Integer buyerSignStatus;
    private Long buyerSealId;
    private Long buyerOperatorId;
    private Integer buyerVerifyMethod;
    private LocalDateTime buyerSignAt;
    private String buyerSignIp;
    private String buyerSignDevice;
    private Integer buyerSignChannel;
    private String unsignedPdfUrl;
    private String signedPdfUrl;
    private String evidenceNo;
    private String blockchainHash;
    private Integer overallStatus;
    private LocalDateTime completedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
