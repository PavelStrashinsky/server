package com.bank.core.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionRequestDTO {
    @NotNull
    private Long cardId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Сумма транзакции должна быть больше 0")
    private BigDecimal amount;

    @NotNull
    private String mccCode;

    private String description;
}