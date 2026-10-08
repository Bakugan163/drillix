package com.example.drillix.Equipment;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EquipmentRequestDto (

    @NotNull (message="Укажите тип оборудования")
    EquipmentType type,

    @NotBlank (message="Укажите модель")
    String name,

    @NotNull (message="Укажите диаметр")
    @Positive(message = "Диаметр должен быть положительным")
    BigDecimal diameter,


    String description

    )
    
{}
