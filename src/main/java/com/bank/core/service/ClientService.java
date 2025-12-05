package com.bank.core.service;

import com.bank.core.domain.*;
import com.bank.core.domain.enums.CardStatus;
import com.bank.core.domain.enums.LoanStatus;
import com.bank.core.dto.*;
import com.bank.core.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final CardRepository cardRepository;
    private final BonusAccountRepository bonusAccountRepository;
    private final BonusLedgerRepository bonusLedgerRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;

    @Transactional(readOnly = true)
    public ClientDashboardDTO getDashboard(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));

        BonusAccount bonusAccount = bonusAccountRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("Счет бонусов не найден"));

        List<Card> cards = cardRepository.findAll().stream()
                .filter(c -> c.getClient().getId().equals(clientId))
                .collect(Collectors.toList());

        List<Loan> loans = loanRepository.findByClientId(clientId);

        ClientDashboardDTO dashboard = new ClientDashboardDTO();

        dashboard.setClientId(client.getId());
        dashboard.setFullName(client.getFullName());
        dashboard.setMonthlyIncome(client.getMonthlyIncome());
        dashboard.setEmploymentStartDate(client.getEmploymentStartDate());
        dashboard.setCreditHistoryScore(client.getCreditHistoryScore() != null ? client.getCreditHistoryScore() : 0);
        dashboard.setRiskClass(client.getRiskClass().name());

        dashboard.setPointsBalance(bonusAccount.getPointsBalance());

        dashboard.setCards(cards.stream().map(c -> {
            CardDTO dto = new CardDTO();
            dto.setId(c.getId());
            dto.setCardNumber(c.getCardNumber());
            dto.setBalance(c.getBalance());
            dto.setCreditLimit(c.getCreditLimit());
            dto.setStatus(c.getStatus().name());
            dto.setExpirationDate(c.getExpirationDate());
            return dto;
        }).collect(Collectors.toList()));

        dashboard.setLoans(loans.stream().map(l -> {
            LoanDTO dto = new LoanDTO();
            dto.setId(l.getId());
            dto.setPrincipalAmount(l.getPrincipalAmount());
            dto.setTotalRepay(l.getTotalAmountToRepay());
            dto.setRemainingDebt(l.getRemainingDebt());
            dto.setMonthlyPayment(l.getMonthlyPayment());
            dto.setStatus(l.getStatus().name());
            dto.setNextPaymentDate(l.getStartDate().plusMonths(1));
            return dto;
        }).collect(Collectors.toList()));

        return dashboard;
    }

    @Transactional
    public void payLoan(Long clientId, Long loanId, Long cardId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Кредит не найден"));
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Карта не найдена"));

        if (!loan.getClient().getId().equals(clientId) || !card.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Доступ запрещен");
        }

        if (card.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Недостаточно средств на карте");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Сумма должна быть положительной");
        }

        card.setBalance(card.getBalance().subtract(amount));
        cardRepository.save(card);

        BigDecimal newDebt = loan.getRemainingDebt().subtract(amount);
        if (newDebt.compareTo(BigDecimal.ZERO) <= 0) {
            newDebt = BigDecimal.ZERO;
            loan.setStatus(LoanStatus.PAID);
        }
        loan.setRemainingDebt(newDebt);
        loanRepository.save(loan);
    }

    @Transactional(readOnly = true)
    public List<BonusHistoryDTO> getBonusHistory(Long clientId) {
        BonusAccount account = bonusAccountRepository.findByClientId(clientId).orElseThrow();
        return bonusLedgerRepository.findByBonusAccountIdOrderByCreatedAtDesc(account.getId())
                .stream().map(l -> {
                    BonusHistoryDTO dto = new BonusHistoryDTO();
                    dto.setDate(l.getCreatedAt());
                    dto.setAmount(l.getAmount());
                    dto.setType(l.getType().name());
                    dto.setDescription(l.getDescription());
                    return dto;
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionHistoryDTO> getTransactionHistory(Long clientId) {
        return transactionRepository.findAllByClientId(clientId).stream()
                .map(t -> {
                    TransactionHistoryDTO dto = new TransactionHistoryDTO();
                    dto.setId(t.getId());
                    dto.setCardNumber("**** " + t.getCard().getCardNumber().substring(12));
                    dto.setAmount(t.getAmount());
                    dto.setMccCode(t.getMccCode());
                    dto.setStatus(t.getStatus().name());
                    dto.setCreatedAt(t.getCreatedAt());
                    dto.setDescription(t.getDescription() != null ? t.getDescription() : "Покупка");
                    return dto;
                }).collect(Collectors.toList());
    }
}