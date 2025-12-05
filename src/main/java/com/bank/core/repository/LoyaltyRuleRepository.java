package com.bank.core.repository;

import com.bank.core.domain.LoyaltyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoyaltyRuleRepository extends JpaRepository<LoyaltyRule, Long> {
    Optional<LoyaltyRule> findByMccCode(String mccCode);
}