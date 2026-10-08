package com.example.drillix.Well;

import java.time.LocalDate;
import java.time.LocalDateTime;

//исходящее DTO, то как объект выглядит на выходе
public record WellResponseDto (
    Long id,
    String field,
    String wellNumber,
    WellType wellType,
    Integer projectDepth,
    LocalDate drillinggStartDate,
    LocalDate completionDate,
    LocalDateTime createdAt
){
    //логика конвертации из Entity в DTO
    public static WellResponseDto from(Well well) {
        return new WellResponseDto(
            well.getId(),
            well.getField(),
            well.getWellNumber(),
            well.getWellType(),
            well.getProjectDepth(),
            well.getDrillingStartDate(),
            well.getCompletionDate(),
            well.getCreatedAt()
        );
    }
}
