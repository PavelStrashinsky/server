package com.bank.core.domain;

import com.bank.core.domain.enums.RiskClass;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "clients")
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String passport;

    @Column(name = "monthly_income", nullable = false)
    private BigDecimal monthlyIncome;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "employment_start_date")
    private LocalDate employmentStartDate;

    @Column(name = "marital_status")
    private String maritalStatus;

    @Column(name = "credit_history_score")
    private Integer creditHistoryScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_class")
    private RiskClass riskClass = RiskClass.NONE;
}