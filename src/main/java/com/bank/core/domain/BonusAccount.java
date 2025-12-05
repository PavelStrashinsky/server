package com.bank.core.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "bonus_accounts")
public class BonusAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", unique = true, nullable = false)
    private Client client;

    @Column(name = "points_balance", nullable = false)
    private Integer pointsBalance = 0;
}