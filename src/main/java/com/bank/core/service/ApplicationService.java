package com.bank.core.service;

import com.bank.core.domain.Client;
import com.bank.core.domain.CreditApplication;
import com.bank.core.domain.enums.ApplicationStatus;
import com.bank.core.domain.enums.RiskClass;
import com.bank.core.dto.ApplicationRequestDTO;
import com.bank.core.dto.ScoringResultDTO;
import com.bank.core.repository.ClientRepository;
import com.bank.core.repository.CreditApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ScoringService scoringService;
    private final CreditApplicationRepository applicationRepository;
    private final ClientRepository clientRepository;

    @Transactional
    public String processApplication(ApplicationRequestDTO request) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));

        ScoringResultDTO scoringResult = scoringService.calculate(request, client);

        client.setCreditHistoryScore(scoringResult.getScore());
        client.setRiskClass(scoringResult.getRiskClass());
        clientRepository.save(client);

        CreditApplication app = new CreditApplication();
        app.setClient(client);
        app.setRequestedLimit(request.getRequestedLimit());
        app.setCalculatedScore(scoringResult.getScore());

        app.setApprovedMinLimit(scoringResult.getMinLimit());
        app.setApprovedMaxLimit(scoringResult.getMaxLimit());

        double years = 0;
        if (client.getEmploymentStartDate() != null) {
            years = ChronoUnit.MONTHS.between(client.getEmploymentStartDate(), LocalDateTime.now().toLocalDate()) / 12.0;
        }
        app.setWorkExperienceYears(years);

        app.setTermMonths(request.getTermMonths());

        if (scoringResult.isApproved()) {
            app.setStatus(ApplicationStatus.PENDING);
        } else {
            app.setStatus(ApplicationStatus.REJECTED);
        }

        app.setCreatedAt(LocalDateTime.now());
        applicationRepository.save(app);

        if (scoringResult.isApproved()) {
            if (request.getRequestedLimit().compareTo(scoringResult.getMaxLimit()) > 0) {
                return String.format("Одобрено с ограничением. Доступно: %s - %s BYN.", scoringResult.getMinLimit(), scoringResult.getMaxLimit());
            } else {
                return String.format("Предварительно одобрено! Лимит: %s - %s BYN.", scoringResult.getMinLimit(), scoringResult.getMaxLimit());
            }
        } else {
            return "Отказано. Ваш скоринговый балл: " + scoringResult.getScore();
        }
    }
}