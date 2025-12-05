package com.bank.core.service;

import com.bank.core.domain.*;
import com.bank.core.domain.enums.ApplicationStatus;
import com.bank.core.domain.enums.CardStatus;
import com.bank.core.dto.AuthDTOs;
import com.bank.core.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final LoyaltyRuleRepository loyaltyRuleRepository;
    private final UserRepository userRepository;
    private final CreditApplicationRepository applicationRepository;
    private final ClientRepository clientRepository;
    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;

    private final EmployeeService employeeService;

    @Transactional(readOnly = true)
    public List<LoyaltyRule> getAllLoyaltyRules() {
        return loyaltyRuleRepository.findAll();
    }

    @Transactional
    public LoyaltyRule addLoyaltyRule(LoyaltyRule rule) {
        return loyaltyRuleRepository.save(rule);
    }

    @Transactional
    public void deleteLoyaltyRule(Long id) {
        loyaltyRuleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CreditApplication> getAllApplications() {
        return applicationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AuthDTOs.UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(u -> {
            AuthDTOs.UserDTO dto = new AuthDTOs.UserDTO();
            dto.setId(u.getId());
            dto.setUsername(u.getUsername());
            dto.setRole(u.getRole());
            dto.setEnabled(u.isEnabled());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void approveUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void toggleUserBlock(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Client getClientByUserId(Long userId) {
        return clientRepository.findByUserId(userId).orElse(null);
    }

    @Transactional
    public void updateClientData(Long userId, Client newData) {
        Client client = clientRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Профиль клиента не найден"));

        client.setFullName(newData.getFullName());
        client.setPassport(newData.getPassport());
        client.setMonthlyIncome(newData.getMonthlyIncome());
        clientRepository.save(client);
    }

    @Transactional(readOnly = true)
    public List<Card> getUserCards(Long userId) {
        Client client = clientRepository.findByUserId(userId).orElse(null);
        if (client == null) return List.of();
        return cardRepository.findAll().stream()
                .filter(c -> c.getClient().getId().equals(client.getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void toggleCardBlock(Long cardId) {
        Card card = cardRepository.findById(cardId).orElseThrow();
        if (card.getStatus() == CardStatus.ACTIVE) card.setStatus(CardStatus.BLOCKED);
        else if (card.getStatus() == CardStatus.BLOCKED) card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
    }

    @Transactional
    public void createUser(AuthDTOs.RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Пользователь уже существует");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole().toUpperCase() : "CLIENT");
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void forceApproveApplication(Long applicationId, BigDecimal finalLimit) {
        CreditApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        app.setFinalApprovedLimit(finalLimit);
        app.setStatus(ApplicationStatus.APPROVED);
        applicationRepository.save(app);

        int term = app.getTermMonths() != null ? app.getTermMonths() : 12;
        employeeService.issueProduct(app.getClient(), finalLimit, term);
    }
}