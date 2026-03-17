package com.autorecon.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Excel 导入对账单明细 DTO
 */
@Data
public class BillItemExcelDTO {

    @ExcelProperty("合同编号")
    private String contractNo;
    @ExcelProperty("订单号")
    private String orderNo;
    @ExcelProperty("发货单号")
    private String deliveryNo;
    @ExcelProperty("品名")
    private String productName;
    @ExcelProperty("规格")
    private String spec;
    @ExcelProperty("材质")
    private String material;
    @ExcelProperty("数量")
    private BigDecimal quantity;
    @ExcelProperty("重量")
    private BigDecimal weight;
    @ExcelProperty("单价")
    private BigDecimal unitPrice;
    @ExcelProperty("金额")
    private BigDecimal amount;
    @ExcelProperty("发货日期")
    private LocalDate deliveryDate;
}
