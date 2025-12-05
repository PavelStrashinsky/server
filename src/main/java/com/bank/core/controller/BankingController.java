package com.bank.core.controller;

import com.bank.core.dto.*;
import com.bank.core.service.ApplicationService;
import com.bank.core.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BankingController {

    private final ApplicationService applicationService;
    private final TransactionService transactionService;

    @PostMapping("/applications/apply")
    public ResponseEntity<String> submitApplication(@Valid @RequestBody ApplicationRequestDTO request) {
        String result = applicationService.processApplication(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/transactions/simulate")
    public ResponseEntity<TransactionResponseDTO> simulateTransaction(@Valid @RequestBody TransactionRequestDTO request) {
        TransactionResponseDTO response = transactionService.processTransaction(request);
        return ResponseEntity.ok(response);
    }
}