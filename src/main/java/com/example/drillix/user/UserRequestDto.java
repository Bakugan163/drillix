package com.example.drillix.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRequestDto(

        @NotBlank(message = "Укажите логин")
        String username,

        @NotBlank(message = "Укажите пароль")
        String password,

        @NotNull(message = "Укажите роль: ROLE_ADMIN или ROLE_TECHNOLOGIST")
        UserRole role
) {}