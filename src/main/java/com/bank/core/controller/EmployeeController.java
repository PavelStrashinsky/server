package com.bank.core.controller;

import com.bank.core.domain.Client;
import com.bank.core.domain.CreditApplication;
import com.bank.core.dto.CreditApplicationDTO;
import com.bank.core.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/clients")
    public ResponseEntity<List<Client>> getAllClients() {
        return ResponseEntity.ok(employeeService.getAllClients());
    }

    @PutMapping("/clients/{id}")
    public ResponseEntity<Client> updateClient(@PathVariable Long id, @RequestBody Client client) {
        return ResponseEntity.ok(employeeService.updateClient(id, client));
    }

    @GetMapping("/applications/pending")
    public ResponseEntity<List<CreditApplicationDTO>> getPendingApplications() {
        List<CreditApplicationDTO> dtos = employeeService.getPendingApplications().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/applications/{id}/approve")
    public ResponseEntity<String> approveApplication(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        BigDecimal limit = body.get("finalLimit");
        if (limit == null) {
            return ResponseEntity.badRequest().body("Не указан finalLimit");
        }
        try {
            employeeService.approveApplication(id, limit);
            return ResponseEntity.ok("Заявка одобрена, карта выпущена/пополнена на " + limit);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/applications/{id}/reject")
    public ResponseEntity<String> rejectApplication(@PathVariable Long id) {
        employeeService.rejectApplication(id);
        return ResponseEntity.ok("Заявка отклонена");
    }

    @PostMapping("/cards/{id}/status")
    public ResponseEntity<String> changeCardStatus(@PathVariable Long id, @RequestParam String status) {
        employeeService.changeCardStatus(id, status);
        return ResponseEntity.ok("Статус карты изменен");
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