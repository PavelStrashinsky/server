package com.bank.core.repository;

import com.bank.core.domain.BonusAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BonusAccountRepository extends JpaRepository<BonusAccount, Long> {
    Optional<BonusAccount> findByClientId(Long clientId);
}