package com.bank.core.repository;

import com.bank.core.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.card.client.id = :clientId ORDER BY t.createdAt DESC")
    List<Transaction> findAllByClientId(@Param("clientId") Long clientId);
}