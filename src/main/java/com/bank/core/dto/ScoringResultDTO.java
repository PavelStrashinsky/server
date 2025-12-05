package com.bank.core.dto;

import com.bank.core.domain.enums.RiskClass;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ScoringResultDTO {
    private int score;
    private boolean approved;
    private BigDecimal minLimit;
    private BigDecimal maxLimit;
    private RiskClass riskClass;
    private String message;
}