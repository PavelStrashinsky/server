package com.bank.core.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CardDTO {
    private Long id;
    private String cardNumber;
    private BigDecimal balance;
    private BigDecimal creditLimit;
    private String status;
    private LocalDate expirationDate;
}
