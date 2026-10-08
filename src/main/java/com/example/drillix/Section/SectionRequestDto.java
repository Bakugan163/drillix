package com.example.drillix.Section;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SectionRequestDto (
    
    @NotNull (message="Укажите тип скважины")
    SectionType type,

    @NotNull (message="Укажите начало интервала")
    Integer depthFrom,

    @NotNull (message="Укажите забой")
    Integer depthTo,

    @NotNull (message="Укажите диаметр долота")
    @Positive (message="Диаметр должен быть положительным")
    BigDecimal bitDiametr,

    @NotNull (message="Укажите задачу по траектории")
    TrajectoryTask trajectoryTask,

    @DecimalMin(value = "0.0", message = "Зенитный угол не может быть отрицательным")
    @DecimalMax(value = "180.0", message = "Зенитный угол не может превышать 180°")
    BigDecimal maxZenithAngele,

    String drillingConditions
)

{}
