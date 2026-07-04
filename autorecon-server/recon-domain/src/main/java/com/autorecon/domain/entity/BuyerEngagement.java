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
 * 买方参与等级
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("buyer_engagement")
public class BuyerEngagement {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long buyerEnterpriseId;
    private Long sellerEnterpriseId;
    private String buyerPhone;
    private String buyerContactName;
    private String buyerCompanyName;
    private Integer engagementLevel;
    private LocalDateTime firstLinkSentAt;
    private LocalDateTime firstLinkOpenedAt;
    private LocalDateTime registeredAt;
    private LocalDateTime firstDataSubmitAt;
    private LocalDateTime sealInitializedAt;
    private LocalDateTime erpConnectedAt;
    private Integer totalBillsSent;
    private Integer totalBillsConfirmed;
    private Integer totalBillsIgnored;
    private LocalDateTime lastActiveAt;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
