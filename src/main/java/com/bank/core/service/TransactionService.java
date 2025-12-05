package com.bank.core.service;

import com.bank.core.domain.Card;
import com.bank.core.domain.Transaction;
import com.bank.core.domain.enums.CardStatus;
import com.bank.core.domain.enums.TransactionStatus;
import com.bank.core.dto.TransactionRequestDTO;
import com.bank.core.dto.TransactionResponseDTO;
import com.bank.core.repository.CardRepository;
import com.bank.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final LoyaltyService loyaltyService;

    @Transactional
    public TransactionResponseDTO processTransaction(TransactionRequestDTO request) {
        Card card = cardRepository.findById(request.getCardId())
                .orElseThrow(() -> new RuntimeException("Карта не найдена"));

        TransactionResponseDTO response = new TransactionResponseDTO();

        if (card.getStatus() != CardStatus.ACTIVE) {
            response.setStatus("FAILED");
            response.setMessage("Карта заблокирована или истекла");
            return response;
        }

        if (card.getBalance().compareTo(request.getAmount()) < 0) {
            response.setStatus("FAILED");
            response.setMessage("Недостаточно средств");
            response.setCurrentBalance(card.getBalance());
            return response;
        }

        card.setBalance(card.getBalance().subtract(request.getAmount()));
        cardRepository.save(card);

        Transaction transaction = new Transaction();
        transaction.setCard(card);
        transaction.setAmount(request.getAmount());
        transaction.setMccCode(request.getMccCode());

        transaction.setStatus(TransactionStatus.COMPLETED);

        transaction.setCreatedAt(LocalDateTime.now());
        Transaction savedTransaction = transactionRepository.save(transaction);

        loyaltyService.applyLoyaltyProgram(savedTransaction);

        Card updatedCard = cardRepository.findById(card.getId()).orElseThrow();

        response.setTransactionId(savedTransaction.getId());
        response.setStatus("COMPLETED");
        response.setMessage("Успешно");
        response.setCurrentBalance(updatedCard.getBalance());

        return response;
    }
}