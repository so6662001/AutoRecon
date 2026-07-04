package com.pickupexpress.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ERP 合同同步请求体
 */
@Data
public class ErpContractSyncDTO {

    private String contractNo;
    private String erpContractId;
    private Integer contractType;
    private String buyerName;
    private String buyerContactName;
    private String buyerContactPhone;
    private BigDecimal totalQuantity;
    private BigDecimal totalWeight;
    private BigDecimal totalAmount;
    private String paymentTerms;
    private LocalDate deliveryDeadline;
    private String warehouseName;
    private List<ErpContractItemDTO> items;
}
