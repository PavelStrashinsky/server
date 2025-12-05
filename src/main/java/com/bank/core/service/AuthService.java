package com.bank.core.service;

import com.bank.core.domain.*;
import com.bank.core.domain.enums.CardStatus;
import com.bank.core.dto.AuthDTOs;
import com.bank.core.repository.*;
import com.bank.core.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final BonusAccountRepository bonusAccountRepository;
    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final ScoringService scoringService;

    @Transactional
    public AuthDTOs.AuthResponse register(AuthDTOs.RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Пользователь с таким логином уже существует");
        }

        String role = request.getRole() != null ? request.getRole().toUpperCase() : "CLIENT";

        if ("CLIENT".equals(role)) {
            if (request.getPassport() == null || request.getPassport().isEmpty()) throw new RuntimeException("Паспорт обязателен");
            if (request.getMonthlyIncome() == null) throw new RuntimeException("Доход обязателен");
            if (request.getBirthDate() == null) throw new RuntimeException("Дата рождения обязательна");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setEnabled(!"EMPLOYEE".equals(role));

        User savedUser = userRepository.save(user);
        Long clientId = null;

        if ("CLIENT".equals(role)) {
            Client client = new Client();
            client.setUserId(savedUser.getId());
            client.setFullName(request.getFullName());
            client.setPassport(request.getPassport());
            client.setMonthlyIncome(request.getMonthlyIncome());
            client.setBirthDate(request.getBirthDate());
            client.setEmploymentStartDate(LocalDate.now().minusYears(1));
            client.setMaritalStatus(request.getMaritalStatus() != null ? request.getMaritalStatus() : "SINGLE");

            Client savedClient = clientRepository.save(client);
            clientId = savedClient.getId();

            scoringService.performCurrentScoring(savedClient);

            BonusAccount bonusAccount = new BonusAccount();
            bonusAccount.setClient(savedClient);
            bonusAccount.setPointsBalance(0);
            bonusAccountRepository.save(bonusAccount);

            createDefaultCard(savedClient);
        }

        if (!savedUser.isEnabled()) {
            AuthDTOs.AuthResponse response = new AuthDTOs.AuthResponse();
            response.setUsername(savedUser.getUsername());
            response.setRole(savedUser.getRole());
            return response;
        }

        String token = tokenProvider.generateToken(savedUser.getUsername(), savedUser.getRole(), savedUser.getId(), clientId);

        AuthDTOs.AuthResponse response = new AuthDTOs.AuthResponse();
        response.setToken(token);
        response.setUsername(savedUser.getUsername());
        response.setRole(savedUser.getRole());
        response.setClientId(clientId);
        return response;
    }

    public AuthDTOs.AuthResponse login(AuthDTOs.LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Аккаунт не активирован");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Неверный пароль");
        }

        Long clientId = null;
        if ("CLIENT".equals(user.getRole())) {
            clientId = clientRepository.findByUserId(user.getId()).map(Client::getId).orElse(null);
        }

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole(), user.getId(), clientId);

        AuthDTOs.AuthResponse response = new AuthDTOs.AuthResponse();
        response.setToken(token);
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setClientId(clientId);
        return response;
    }

    private void createDefaultCard(Client client) {
        Card card = new Card();
        card.setClient(client);
        Random random = new Random();
        String randomDigits = String.format("%012d", Math.abs(random.nextLong()) % 1000000000000L);
        card.setCardNumber("4200" + randomDigits);
        card.setBalance(BigDecimal.ZERO);
        card.setCreditLimit(BigDecimal.ZERO);
        card.setStatus(CardStatus.ACTIVE);
        card.setExpirationDate(LocalDate.now().plusYears(4));
        card.setCvvHash("123");
        cardRepository.save(card);
    }
}