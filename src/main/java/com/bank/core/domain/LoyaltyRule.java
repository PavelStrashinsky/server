package com.bank.core.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "loyalty_rules")
public class LoyaltyRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mcc_code", unique = true, nullable = false)
    private String mccCode;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "cashback_rate", precision = 5, scale = 4)
    private BigDecimal cashbackRate;

    @Column(name = "is_bonus_points")
    private Boolean isBonusPoints;
}