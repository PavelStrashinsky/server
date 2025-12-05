package com.bank.core.repository;

import com.bank.core.domain.Loan;
import com.bank.core.domain.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByClientId(Long clientId);

    List<Loan> findByClientIdAndStatus(Long clientId, LoanStatus status);
}