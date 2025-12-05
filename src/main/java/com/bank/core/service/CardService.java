package com.bank.core.service;

import com.bank.core.domain.Card;
import com.bank.core.domain.Transaction;
import com.bank.core.domain.enums.CardStatus;
import com.bank.core.domain.enums.TransactionStatus;
import com.bank.core.repository.CardRepository;
import com.bank.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void toggleBlockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Карта не найдена"));
        if (card.getStatus() == CardStatus.ACTIVE) {
            card.setStatus(CardStatus.BLOCKED);
        } else if (card.getStatus() == CardStatus.BLOCKED) {
            card.setStatus(CardStatus.ACTIVE);
        }
        cardRepository.save(card);
    }

    @Transactional
    public void topUpCard(Long cardId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("Сумма должна быть положительной");

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Карта не найдена"));

        card.setBalance(card.getBalance().add(amount));
        cardRepository.save(card);

        recordTransaction(card, amount, "TOPUP", "Пополнение счета");
    }

    @Transactional
    public void p2pTransfer(Long senderCardId, String receiverCardNumber, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("Сумма должна быть положительной");

        Card sender = cardRepository.findById(senderCardId)
                .orElseThrow(() -> new RuntimeException("Карта отправителя не найдена"));

        if (sender.getStatus() != CardStatus.ACTIVE) throw new RuntimeException("Карта отправителя заблокирована");
        if (sender.getBalance().compareTo(amount) < 0) throw new RuntimeException("Недостаточно средств");

        String cleanReceiverNumber = receiverCardNumber.replaceAll("[^0-9]", "");

        Card receiver = cardRepository.findByCardNumber(cleanReceiverNumber)
                .orElseThrow(() -> new RuntimeException("Карта получателя не найдена (" + cleanReceiverNumber + ")"));

        if (sender.getId().equals(receiver.getId())) throw new RuntimeException("Нельзя перевести самому себе на ту же карту");

        sender.setBalance(sender.getBalance().subtract(amount));
        cardRepository.save(sender);
        recordTransaction(sender, amount, "P2P_OUT", "Перевод на карту " + cleanReceiverNumber);

        receiver.setBalance(receiver.getBalance().add(amount));
        cardRepository.save(receiver);
        recordTransaction(receiver, amount, "P2P_IN", "Перевод от " + sender.getClient().getFullName());
    }

    private void recordTransaction(Card card, BigDecimal amount, String mcc, String desc) {
        Transaction t = new Transaction();
        t.setCard(card);
        t.setAmount(amount);
        t.setMccCode(mcc);
        t.setDescription(desc);
        t.setStatus(TransactionStatus.COMPLETED);
        t.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(t);
    }
}