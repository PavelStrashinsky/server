package com.bank.core.controller;

import com.bank.core.domain.*;
import com.bank.core.dto.AuthDTOs;
import com.bank.core.dto.CreditApplicationDTO;
import com.bank.core.repository.SystemParameterRepository;
import com.bank.core.service.AdminService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;
    private final SystemParameterRepository systemParameterRepository;

    @GetMapping("/users")
    public ResponseEntity<List<AuthDTOs.UserDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/users/{id}/approve")
    public ResponseEntity<String> approveUser(@PathVariable Long id) {
        adminService.approveUser(id);
        return ResponseEntity.ok("Пользователь активирован");
    }

    @PostMapping("/users/{id}/toggle-block")
    public ResponseEntity<String> toggleUserBlock(@PathVariable Long id) {
        adminService.toggleUserBlock(id);
        return ResponseEntity.ok("Статус пользователя изменен");
    }

    @PostMapping("/users/create")
    public ResponseEntity<String> createUser(@RequestBody AuthDTOs.RegisterRequest request) {
        adminService.createUser(request);
        return ResponseEntity.ok("Пользователь успешно создан");
    }

    @GetMapping("/users/{id}/client")
    public ResponseEntity<Client> getClientProfile(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getClientByUserId(id));
    }

    @PutMapping("/users/{id}/client")
    public ResponseEntity<String> updateClientProfile(@PathVariable Long id, @RequestBody Client client) {
        adminService.updateClientData(id, client);
        return ResponseEntity.ok("Профиль обновлен");
    }

    @GetMapping("/users/{id}/cards")
    public ResponseEntity<List<Card>> getUserCards(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserCards(id));
    }

    @PostMapping("/cards/{cardId}/toggle")
    public ResponseEntity<String> toggleCardBlock(@PathVariable Long cardId) {
        adminService.toggleCardBlock(cardId);
        return ResponseEntity.ok("Статус карты изменен");
    }

    @GetMapping("/loyalty")
    public ResponseEntity<List<LoyaltyRule>> getLoyaltyRules() {
        return ResponseEntity.ok(adminService.getAllLoyaltyRules());
    }

    @PostMapping("/loyalty")
    public ResponseEntity<LoyaltyRule> addLoyaltyRule(@RequestBody LoyaltyRule rule) {
        return ResponseEntity.ok(adminService.addLoyaltyRule(rule));
    }

    @DeleteMapping("/loyalty/{id}")
    public ResponseEntity<Void> deleteLoyaltyRule(@PathVariable Long id) {
        adminService.deleteLoyaltyRule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/params")
    public ResponseEntity<List<SystemParameter>> getParams() {
        return ResponseEntity.ok(systemParameterRepository.findAll());
    }

    @PostMapping("/params")
    public ResponseEntity<SystemParameter> updateParam(@RequestBody SystemParameter param) {
        return ResponseEntity.ok(systemParameterRepository.save(param));
    }

    @GetMapping("/applications")
    public ResponseEntity<List<CreditApplicationDTO>> getAllApplications() {
        List<CreditApplicationDTO> dtos = adminService.getAllApplications().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/applications/{id}/approve-force")
    public ResponseEntity<String> forceApprove(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        BigDecimal limit = body.get("finalLimit");
        adminService.forceApproveApplication(id, limit);
        return ResponseEntity.ok("Заявка принудительно одобрена админом");
    }

    @GetMapping("/applications/export")
    public void exportApplicationsToCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.addHeader("Content-Disposition", "attachment; filename=\"applications_report.csv\"");
        response.getWriter().write('\uFEFF');

        PrintWriter writer = response.getWriter();
        writer.println("ID;Client;Passport;Income;Requested;Approved;Score;Term;Status;Date");

        List<CreditApplication> apps = adminService.getAllApplications();

        for (CreditApplication app : apps) {
            writer.printf("%d;%s;%s;%s;%s;%s;%d;%d;%s;%s%n",
                    app.getId(),
                    app.getClient().getFullName(),
                    app.getClient().getPassport(),
                    app.getClient().getMonthlyIncome(),
                    app.getRequestedLimit(),
                    app.getFinalApprovedLimit() != null ? app.getFinalApprovedLimit() : "0",
                    app.getCalculatedScore(),
                    app.getTermMonths(),
                    app.getStatus(),
                    app.getCreatedAt().toString()
            );
        }
    }

    private CreditApplicationDTO convertToDTO(CreditApplication app) {
        CreditApplicationDTO dto = new CreditApplicationDTO();
        dto.setId(app.getId());
        dto.setClientId(app.getClient().getId());
        dto.setClientName(app.getClient().getFullName());
        dto.setClientPassport(app.getClient().getPassport());
        dto.setClientIncome(app.getClient().getMonthlyIncome());
        dto.setRequestedLimit(app.getRequestedLimit());
        dto.setApprovedMinLimit(app.getApprovedMinLimit());
        dto.setApprovedMaxLimit(app.getApprovedMaxLimit());
        dto.setFinalApprovedLimit(app.getFinalApprovedLimit());
        dto.setCalculatedScore(app.getCalculatedScore());
        dto.setWorkExperienceYears(app.getWorkExperienceYears());
        dto.setTermMonths(app.getTermMonths());
        dto.setStatus(app.getStatus().name());
        dto.setCreatedAt(app.getCreatedAt());
        return dto;
    }
}