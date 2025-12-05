package com.bank.core.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LoanDTO {
    private Long id;
    private BigDecimal principalAmount;
    private BigDecimal totalRepay;
    private BigDecimal remainingDebt;
    private BigDecimal monthlyPayment;
    private LocalDate nextPaymentDate;
    private String status;
}