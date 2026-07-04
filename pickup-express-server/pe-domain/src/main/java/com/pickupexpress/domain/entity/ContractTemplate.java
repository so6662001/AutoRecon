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
 * 合同模板
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("contract_template")
public class ContractTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long enterpriseId;
    private String templateName;
    private Integer contractType;
    private Integer templateSource;
    private String templateContent;
    private String templateFileUrl;
    private String fieldMapping;
    private Integer hasOriginField;
    private Integer platformTermsPriority;
    private String applicableBuyers;
    private Integer isDefault;
    private Integer version;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
