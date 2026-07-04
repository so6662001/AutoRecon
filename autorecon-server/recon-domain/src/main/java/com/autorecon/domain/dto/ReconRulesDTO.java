package com.autorecon.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReconRulesDTO {

    /** Weight tolerance as percentage, e.g. 0.5 means 0.5% */
    @DecimalMin("0")
    private BigDecimal weightTolerancePercent;

    /** Absolute amount tolerance (same currency as bills) */
    @DecimalMin("0")
    private BigDecimal amountToleranceAbs;

    /** Date tolerance in calendar days */
    @Min(0)
    private Integer dateToleranceDays;

    /** Auto-confirm when within tolerance */
    private Boolean autoConfirmWithinTolerance;

    /** Default deduction strategy label */
    private String defaultDeductionStrategy;
}
