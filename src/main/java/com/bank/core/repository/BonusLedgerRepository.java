package com.bank.core.repository;
import com.bank.core.domain.BonusLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BonusLedgerRepository extends JpaRepository<BonusLedger, Long> {
    List<BonusLedger> findByBonusAccountIdOrderByCreatedAtDesc(Long bonusAccountId);
}