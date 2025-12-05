package com.bank.core.service;

import com.bank.core.domain.*;
import com.bank.core.domain.enums.BonusType;
import com.bank.core.domain.enums.RiskClass;
import com.bank.core.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyRuleRepository ruleRepository;
    private final BonusAccountRepository bonusAccountRepository;
    private final BonusLedgerRepository bonusLedgerRepository;
    private final CardRepository cardRepository;
    private final SystemParameterRepository paramRepository;

    @Transactional
    public void applyLoyaltyProgram(Transaction transaction) {
        LoyaltyRule rule = ruleRepository.findByMccCode(transaction.getMccCode()).orElse(null);
        if (rule == null) return;

        Card card = transaction.getCard();
        Client client = card.getClient();
        BonusAccount bonusAccount = bonusAccountRepository.findByClientId(client.getId())
                .orElseThrow(() -> new RuntimeException("Нет бонусного счета"));

        BigDecimal amount = transaction.getAmount();

        if (Boolean.TRUE.equals(rule.getIsBonusPoints())) {
            int points = amount.divide(BigDecimal.valueOf(10), 0, RoundingMode.DOWN).intValue();
            if (points > 0) {
                bonusAccount.setPointsBalance(bonusAccount.getPointsBalance() + points);
                bonusAccountRepository.save(bonusAccount);
                recordHistory(bonusAccount, transaction, BigDecimal.valueOf(points), BonusType.POINTS, "Начисление за покупку");
            }
        } else {
            BigDecimal rate = rule.getCashbackRate();
            if (client.getRiskClass() == RiskClass.LOW) rate = rate.multiply(BigDecimal.valueOf(1.5));
            else if (client.getRiskClass() == RiskClass.HIGH) rate = rate.divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);

            BigDecimal cashback = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);

            if (cashback.compareTo(BigDecimal.ZERO) > 0) {
                card.setBalance(card.getBalance().add(cashback));
                cardRepository.save(card);
                recordHistory(bonusAccount, transaction, cashback, BonusType.CASHBACK_RUB, "Кешбэк за покупку");
            }
        }
    }

    @Transactional
    public void convertPointsToMoney(Long clientId, int pointsToConvert) {
        if (pointsToConvert <= 0) throw new RuntimeException("Сумма должна быть положительной");

        BonusAccount account = bonusAccountRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("Счет не найден"));

        if (account.getPointsBalance() < pointsToConvert) {
            throw new RuntimeException("Недостаточно баллов");
        }

        BigDecimal rate = new BigDecimal(paramRepository.findById("POINTS_RATE")
                .map(SystemParameter::getParamValue).orElse("10"));

        BigDecimal money = BigDecimal.valueOf(pointsToConvert)
                .divide(rate, 2, RoundingMode.HALF_UP);

        account.setPointsBalance(account.getPointsBalance() - pointsToConvert);
        bonusAccountRepository.save(account);
        recordHistory(account, null, BigDecimal.valueOf(pointsToConvert).negate(), BonusType.POINTS, "Конвертация в деньги");

        Card card = cardRepository.findAll().stream()
                .filter(c -> c.getClient().getId().equals(clientId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("У клиента нет карт для зачисления"));

        card.setBalance(card.getBalance().add(money));
        cardRepository.save(card);
    }

    private void recordHistory(BonusAccount account, Transaction tx, BigDecimal amount, BonusType type, String desc) {
        BonusLedger ledger = new BonusLedger();
        ledger.setBonusAccount(account);
        ledger.setTransaction(tx);
        ledger.setAmount(amount);
        ledger.setType(type);
        ledger.setDescription(desc);
        bonusLedgerRepository.save(ledger);
    }
}