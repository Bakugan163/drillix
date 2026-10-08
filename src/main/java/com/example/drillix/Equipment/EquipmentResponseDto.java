package com.example.drillix.Equipment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EquipmentResponseDto (
    Long id,
    EquipmentType type,
    String name,
    BigDecimal diameter,
    String description,
    LocalDateTime createdAt
    
) {
    public static EquipmentResponseDto from(Equipment equipment) {
        return new EquipmentResponseDto(
            equipment.getId(),
            equipment.getType(),
            equipment.getName(),
            equipment.getDiameter(),
            equipment.getDescription(),
            equipment.getCreatedAt()
        );
    }
}

