package com.bank.core.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BonusHistoryDTO {
    private LocalDateTime date;
    private String type;
    private BigDecimal amount;
    private String description;
}