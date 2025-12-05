package com.bank.core.domain;

import com.bank.core.domain.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credit_applications")
public class CreditApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "requested_limit", nullable = false)
    private BigDecimal requestedLimit;

    @Column(name = "approved_min_limit")
    private BigDecimal approvedMinLimit;

    @Column(name = "approved_max_limit")
    private BigDecimal approvedMaxLimit;

    @Column(name = "final_approved_limit")
    private BigDecimal finalApprovedLimit;

    @Column(name = "calculated_score")
    private Integer calculatedScore;

    @Column(name = "work_experience_years")
    private Double workExperienceYears;

    @Column(name = "term_months")
    private Integer termMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}