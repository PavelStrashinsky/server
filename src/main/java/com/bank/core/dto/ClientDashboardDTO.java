package com.bank.core.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ClientDashboardDTO {
    private Long clientId;
    private String fullName;
    private Integer pointsBalance;

    private BigDecimal monthlyIncome;
    private LocalDate employmentStartDate;
    private Integer creditHistoryScore;
    private String riskClass;

    private List<CardDTO> cards;
    private List<LoanDTO> loans;
}