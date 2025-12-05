package com.bank.core.service;

import com.bank.core.domain.Client;
import com.bank.core.domain.Loan;
import com.bank.core.domain.enums.LoanStatus;
import com.bank.core.domain.enums.RiskClass;
import com.bank.core.dto.ApplicationRequestDTO;
import com.bank.core.dto.ScoringResultDTO;
import com.bank.core.repository.ClientRepository;
import com.bank.core.repository.LoanRepository;
import com.bank.core.repository.SystemParameterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScoringService {

    private final SystemParameterRepository paramRepository;
    private final LoanRepository loanRepository;
    private final ClientRepository clientRepository;

    public ScoringResultDTO calculate(ApplicationRequestDTO request, Client client) {
        return calculateInternal(client, request.isHasDelinquency());
    }

    @Transactional
    public void performCurrentScoring(Client client) {
        ScoringResultDTO result = calculateInternal(client, false);

        client.setCreditHistoryScore(result.getScore());
        client.setRiskClass(result.getRiskClass());
        clientRepository.save(client);
    }

    private ScoringResultDTO calculateInternal(Client client, boolean hasExternalDelinquency) {
        BigDecimal bpm = new BigDecimal(paramRepository.findById("BPM")
                .map(p -> p.getParamValue()).orElse("400"));

        int score = 0;

        BigDecimal income = client.getMonthlyIncome();
        if (income.compareTo(bpm.multiply(BigDecimal.valueOf(3))) > 0) score += 35;
        else if (income.compareTo(bpm.multiply(BigDecimal.valueOf(1.5))) >= 0) score += 15;
        else score += 5;

        double yearsExp = 0.0;
        if (client.getEmploymentStartDate() != null) {
            long months = ChronoUnit.MONTHS.between(client.getEmploymentStartDate(), LocalDate.now());
            yearsExp = Math.max(0, months / 12.0);
        }

        if (yearsExp >= 3.0) score += 20;
        else if (yearsExp >= 1.0) score += 10;

        if (hasExternalDelinquency) {
            score -= 60;
        } else {
            score += 30;
        }

        List<Loan> activeLoans = loanRepository.findByClientIdAndStatus(client.getId(), LoanStatus.ACTIVE);
        if (!activeLoans.isEmpty()) {
            score -= 10;

            BigDecimal totalMonthlyPayment = activeLoans.stream()
                    .map(Loan::getMonthlyPayment)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal pti = BigDecimal.ZERO;
            if (income.compareTo(BigDecimal.ZERO) > 0) {
                pti = totalMonthlyPayment.divide(income, 2, RoundingMode.HALF_UP);
            }

            if (pti.compareTo(BigDecimal.valueOf(0.4)) > 0) score -= 30;
            if (pti.compareTo(BigDecimal.valueOf(0.6)) > 0) score -= 50;
        }

        if ("MARRIED".equalsIgnoreCase(client.getMaritalStatus())) score += 10;

        int age = Period.between(client.getBirthDate(), LocalDate.now()).getYears();
        if (age >= 30 && age <= 55) score += 5;

        return makeDecision(score);
    }

    private ScoringResultDTO makeDecision(int score) {
        boolean approved;
        BigDecimal minLimit = BigDecimal.ZERO;
        BigDecimal maxLimit = BigDecimal.ZERO;
        RiskClass riskClass = RiskClass.NONE;
        String message;

        if (score >= 75) {
            approved = true;
            minLimit = BigDecimal.valueOf(5000);
            maxLimit = BigDecimal.valueOf(10000);
            riskClass = RiskClass.LOW;
            message = "Одобрено (Премиум)";
        } else if (score >= 40) {
            approved = true;
            minLimit = BigDecimal.valueOf(1500);
            maxLimit = BigDecimal.valueOf(5000);
            riskClass = RiskClass.MIDDLE;
            message = "Одобрено (Стандарт)";
        } else if (score >= 10) {
            approved = true;
            minLimit = BigDecimal.valueOf(500);
            maxLimit = BigDecimal.valueOf(1500);
            riskClass = RiskClass.HIGH;
            message = "Одобрено (Минимальный)";
        } else {
            approved = false;
            message = "Отказ";
        }

        return ScoringResultDTO.builder()
                .score(score)
                .approved(approved)
                .minLimit(minLimit)
                .maxLimit(maxLimit)
                .riskClass(riskClass)
                .message(message)
                .build();
    }
}