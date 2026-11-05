package com.example.drillix.section;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SectionResponseDto (
    
    Long id,
    Long wellId,
    SectionType type,
    Integer depthFrom,
    Integer depthTo,
    BigDecimal bitDiametr,
    TrajectoryTask trajectoryTask,
    BigDecimal maxZenithAngle,
    String drillingCondition,
    LocalDateTime createdAt
) {
    public static SectionResponseDto from(Section section) {
        return new SectionResponseDto(
                section.getId(),
                section.getWell().getId(),
                section.getType(),
                section.getDepthFrom(),
                section.getDepthTo(),
                section.getBitDiametr(),
                section.getTrajectoryTask(),
                section.getMaxZenithAngle(),
                section.getDrillingCondition(),
                section.getCreatedAt()
        );
    }
}




