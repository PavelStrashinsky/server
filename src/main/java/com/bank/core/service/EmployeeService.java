package com.bank.core.service;

import com.bank.core.domain.*;
import com.bank.core.domain.enums.*;
import com.bank.core.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final ClientRepository clientRepository;
    private final CreditApplicationRepository applicationRepository;
    private final CardRepository cardRepository;
    private final LoanRepository loanRepository;

    private static final BigDecimal DEFAULT_INTEREST_RATE = BigDecimal.valueOf(0.15);

    @Transactional(readOnly = true)
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CreditApplication> getPendingApplications() {
        return applicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.PENDING)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveApplication(Long applicationId, BigDecimal finalLimit) {
        CreditApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        if (!isAdmin) {
            if (finalLimit.compareTo(app.getApprovedMaxLimit()) > 0) {
                throw new RuntimeException("Лимит превышает максимально допустимый для сотрудника (" + app.getApprovedMaxLimit() + ")");
            }
            if (finalLimit.compareTo(app.getApprovedMinLimit()) < 0) {
                throw new RuntimeException("Лимит ниже минимально допустимого (" + app.getApprovedMinLimit() + ")");
            }
        }

        app.setFinalApprovedLimit(finalLimit);
        app.setStatus(ApplicationStatus.APPROVED);
        applicationRepository.save(app);

        updateClientRiskClass(app);

        int term = app.getTermMonths() != null ? app.getTermMonths() : 12;

        issueProduct(app.getClient(), finalLimit, term);
    }

    @Transactional
    public void issueProduct(Client client, BigDecimal limit, int termMonths) {
        Card card = getOrCreateCard(client);
        card.setBalance(card.getBalance().add(limit));
        card.setCreditLimit(BigDecimal.ZERO);
        cardRepository.save(card);

        createLoan(client, limit, termMonths);
    }

    private void createLoan(Client client, BigDecimal principal, int months) {
        Loan loan = new Loan();
        loan.setClient(client);
        loan.setPrincipalAmount(principal);

        BigDecimal interestRate;
        if (months <= 12) {
            interestRate = BigDecimal.valueOf(0.12);
        } else if (months <= 36) {
            interestRate = BigDecimal.valueOf(0.15);
        } else {
            interestRate = BigDecimal.valueOf(0.18);
        }
        loan.setInterestRate(interestRate);

        loan.setStartDate(LocalDate.now());
        loan.setEndDate(LocalDate.now().plusMonths(months));
        loan.setStatus(LoanStatus.ACTIVE);

        BigDecimal years = BigDecimal.valueOf(months).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        BigDecimal interestAmount = principal.multiply(interestRate).multiply(years);

        BigDecimal totalDebt = principal.add(interestAmount);

        loan.setTotalAmountToRepay(totalDebt);
        loan.setRemainingDebt(totalDebt);

        BigDecimal monthly = totalDebt.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        loan.setMonthlyPayment(monthly);

        loanRepository.save(loan);
    }

    private Card getOrCreateCard(Client client) {
        return cardRepository.findAll().stream()
                .filter(c -> c.getClient().getId().equals(client.getId()) && c.getStatus() == CardStatus.ACTIVE)
                .findFirst()
                .orElseGet(() -> {
                    Card newCard = new Card();
                    newCard.setClient(client);
                    newCard.setCardNumber("4200" + String.format("%012d", System.currentTimeMillis() % 1000000000000L));
                    newCard.setCreditLimit(BigDecimal.ZERO);
                    newCard.setBalance(BigDecimal.ZERO);
                    newCard.setStatus(CardStatus.ACTIVE);
                    newCard.setExpirationDate(LocalDate.now().plusYears(3));
                    newCard.setCvvHash("999");
                    return cardRepository.save(newCard);
                });
    }

    @Transactional
    public void rejectApplication(Long applicationId) {
        CreditApplication app = applicationRepository.findById(applicationId).orElseThrow();
        app.setStatus(ApplicationStatus.REJECTED);
        applicationRepository.save(app);
    }

    @Transactional
    public Client updateClient(Long id, Client updatedData) {
        Client client = clientRepository.findById(id).orElseThrow();
        client.setFullName(updatedData.getFullName());
        client.setPassport(updatedData.getPassport());
        client.setMonthlyIncome(updatedData.getMonthlyIncome());
        return clientRepository.save(client);
    }

    @Transactional
    public void changeCardStatus(Long cardId, String status) {
        Card card = cardRepository.findById(cardId).orElseThrow();
        card.setStatus(CardStatus.valueOf(status));
        cardRepository.save(card);
    }

    private void updateClientRiskClass(CreditApplication app) {
        Client client = app.getClient();
        int score = app.getCalculatedScore();
        if (score >= 75) client.setRiskClass(RiskClass.LOW);
        else if (score >= 40) client.setRiskClass(RiskClass.MIDDLE);
        else if (score >= 10) client.setRiskClass(RiskClass.HIGH);
        else client.setRiskClass(RiskClass.NONE);
        clientRepository.save(client);
    }
}