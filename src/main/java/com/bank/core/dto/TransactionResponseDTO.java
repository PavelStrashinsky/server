package com.bank.core.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionResponseDTO {
    private Long transactionId;
    private String status;
    private String message;
    private BigDecimal currentBalance;
}