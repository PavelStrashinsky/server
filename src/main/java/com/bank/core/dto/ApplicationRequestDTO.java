package com.bank.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ApplicationRequestDTO {
    private Long clientId;

    @NotNull(message = "Укажите желаемый лимит")
    @Min(value = 100, message = "Минимальный лимит 100 BYN")
    private BigDecimal requestedLimit;

    @NotNull
    private String maritalStatus;

    private boolean hasDelinquency;

    @Min(value = 0, message = "Стаж не может быть отрицательным")
    private double workExperienceYears;

    @NotNull(message = "Укажите срок кредита")
    @Min(value = 3, message = "Минимум 3 месяца")
    @Max(value = 60, message = "Максимум 60 месяцев")
    private Integer termMonths;
}