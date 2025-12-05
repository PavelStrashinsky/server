package com.bank.core.config;

import com.bank.core.domain.LoyaltyRule;
import com.bank.core.domain.SystemParameter;
import com.bank.core.domain.User;
import com.bank.core.repository.LoyaltyRuleRepository;
import com.bank.core.repository.SystemParameterRepository;
import com.bank.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final LoyaltyRuleRepository loyaltyRuleRepository;
    private final SystemParameterRepository systemParameterRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            admin.setEnabled(true);
            userRepository.save(admin);
        }

        createRuleIfMissing("5411", "Супермаркеты", "0.01", false);
        createRuleIfMissing("5541", "АЗС", "0.05", false);
        createRuleIfMissing("7997", "Партнеры (Спорт)", "0.00", true);

        createParamIfMissing("BPM", "400", "Бюджет прожиточного минимума (BYN)");
        createParamIfMissing("POINTS_RATE", "10", "Курс конвертации баллов (Баллов за 1 BYN)");
    }

    private void createRuleIfMissing(String mcc, String category, String rate, boolean isBonus) {
        if (loyaltyRuleRepository.findByMccCode(mcc).isEmpty()) {
            LoyaltyRule rule = new LoyaltyRule();
            rule.setMccCode(mcc);
            rule.setCategoryName(category);
            rule.setCashbackRate(new BigDecimal(rate));
            rule.setIsBonusPoints(isBonus);
            loyaltyRuleRepository.save(rule);
        }
    }

    private void createParamIfMissing(String key, String value, String desc) {
        if (!systemParameterRepository.existsById(key)) {
            systemParameterRepository.save(new SystemParameter(key, value, desc));
        }
    }
}