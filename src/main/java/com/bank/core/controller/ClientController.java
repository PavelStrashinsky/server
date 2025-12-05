package com.bank.core.controller;

import com.bank.core.domain.Client;
import com.bank.core.domain.User;
import com.bank.core.dto.BonusHistoryDTO;
import com.bank.core.dto.ClientDashboardDTO;
import com.bank.core.dto.TransactionHistoryDTO;
import com.bank.core.repository.ClientRepository;
import com.bank.core.repository.UserRepository;
import com.bank.core.service.CardService;
import com.bank.core.service.ClientService;
import com.bank.core.service.LoyaltyService;
import com.bank.core.service.ScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/client")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ClientController {

    private final ClientService clientService;
    private final CardService cardService;
    private final LoyaltyService loyaltyService;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ScoringService scoringService;

    @GetMapping("/dashboard")
    public ResponseEntity<ClientDashboardDTO> getDashboard(Authentication authentication) {
        return ResponseEntity.ok(clientService.getDashboard(getClientIdFromAuth(authentication)));
    }

    @GetMapping("/bonuses/history")
    public ResponseEntity<List<BonusHistoryDTO>> getBonusHistory(Authentication authentication) {
        return ResponseEntity.ok(clientService.getBonusHistory(getClientIdFromAuth(authentication)));
    }

    @GetMapping("/transactions/history")
    public ResponseEntity<List<TransactionHistoryDTO>> getTransactionHistory(Authentication authentication) {
        return ResponseEntity.ok(clientService.getTransactionHistory(getClientIdFromAuth(authentication)));
    }

    @PostMapping("/cards/{cardId}/block")
    public ResponseEntity<String> blockCard(@PathVariable Long cardId) {
        cardService.toggleBlockCard(cardId);
        return ResponseEntity.ok("Статус карты изменен");
    }

    @PostMapping("/cards/{cardId}/topup")
    public ResponseEntity<String> topUpCard(@PathVariable Long cardId, @RequestBody Map<String, BigDecimal> body) {
        cardService.topUpCard(cardId, body.get("amount"));
        return ResponseEntity.ok("Карта пополнена");
    }

    @PostMapping("/cards/p2p")
    public ResponseEntity<String> p2pTransfer(@RequestBody Map<String, Object> body) {
        Long senderCardId = Long.valueOf(body.get("senderCardId").toString());
        String receiverCardNumber = body.get("receiverCardNumber").toString();
        BigDecimal amount = new BigDecimal(body.get("amount").toString());

        cardService.p2pTransfer(senderCardId, receiverCardNumber, amount);
        return ResponseEntity.ok("Перевод выполнен успешно");
    }

    @PostMapping("/bonuses/convert")
    public ResponseEntity<String> convertPoints(Authentication authentication, @RequestBody Map<String, Integer> body) {
        loyaltyService.convertPointsToMoney(getClientIdFromAuth(authentication), body.get("points"));
        return ResponseEntity.ok("Баллы конвертированы");
    }

    @PostMapping("/profile/update")
    public ResponseEntity<String> updateProfile(Authentication authentication, @RequestBody Map<String, String> body) {
        Long clientId = getClientIdFromAuth(authentication);
        Client client = clientRepository.findById(clientId).orElseThrow();

        boolean needsRecalc = false;

        try {
            if (body.containsKey("income") && body.get("income") != null && !body.get("income").isEmpty()) {
                BigDecimal income = new BigDecimal(body.get("income"));
                if (income.compareTo(BigDecimal.ZERO) > 0) {
                    client.setMonthlyIncome(income);
                    needsRecalc = true;
                }
            }

            if (body.containsKey("employmentDate") && body.get("employmentDate") != null && !body.get("employmentDate").isEmpty()) {
                LocalDate date = LocalDate.parse(body.get("employmentDate"));
                if (date.isBefore(LocalDate.now())) {
                    client.setEmploymentStartDate(date);
                    needsRecalc = true;
                }
            }

            clientRepository.save(client);

            if (needsRecalc) {
                scoringService.performCurrentScoring(client);
            }

            return ResponseEntity.ok("Данные обновлены, кредитный рейтинг пересчитан");
        } catch (DateTimeParseException | NumberFormatException e) {
            return ResponseEntity.badRequest().body("Некорректный формат данных");
        }
    }

    @PostMapping("/loans/{loanId}/pay")
    public ResponseEntity<String> payLoan(Authentication authentication, @PathVariable Long loanId, @RequestBody Map<String, Object> body) {
        Long clientId = getClientIdFromAuth(authentication);
        Long cardId = Long.valueOf(body.get("cardId").toString());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());

        clientService.payLoan(clientId, loanId, cardId, amount);
        return ResponseEntity.ok("Платеж по кредиту выполнен");
    }

    private Long getClientIdFromAuth(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        Client client = clientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Профиль клиента не найден"));
        return client.getId();
    }
}