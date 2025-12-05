package com.bank.core.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionHistoryDTO {
    private Long id;
    private String cardNumber;
    private BigDecimal amount;
    private String mccCode;
    private String description;
    private String status;
    private LocalDateTime createdAt;
}