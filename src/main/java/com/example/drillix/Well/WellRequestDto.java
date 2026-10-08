package com.example.drillix.Well;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

//то что система получает на вход (заполняется пользователем)
public record WellRequestDto (
    @NotBlank (message="Укажите месторождение")
    String field,

    @NotBlank (message="Укажите номер скважины")
    String wellNumber,

    @NotNull (message = "Укажите тип скважины")
    WellType wellType,

    @Positive (message="Проектный забой должен быть положительным")
    Integer projectDepth,

    LocalDate drillingStartDate,

    LocalDate completionDate
) 
{}
