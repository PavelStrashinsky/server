package com.bank.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AuthDTOs {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Логин обязателен")
        private String username;

        @NotBlank(message = "Пароль обязателен")
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Логин не может быть пустым")
        private String username;

        @Size(min = 6, message = "Пароль должен быть не менее 6 символов")
        private String password;

        @NotBlank(message = "ФИО обязательно")
        private String fullName;

        private String passport;
        private BigDecimal monthlyIncome;
        private LocalDate birthDate;
        private String maritalStatus;

        private String role;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String username;
        private String role;
        private Long clientId;
    }

    @Data
    public static class UserDTO {
        private Long id;
        private String username;
        private String role;
        private boolean enabled;
    }
}