package com.bank.core.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreditApplicationDTO {
    private Long id;
    private Long clientId;
    private String clientName;
    private String clientPassport;
    private BigDecimal clientIncome;

    private BigDecimal requestedLimit;
    private BigDecimal approvedMinLimit;
    private BigDecimal approvedMaxLimit;
    private BigDecimal finalApprovedLimit;

    private Integer calculatedScore;
    private Double workExperienceYears;

    private Integer termMonths;

    private String status;
    private LocalDateTime createdAt;
}