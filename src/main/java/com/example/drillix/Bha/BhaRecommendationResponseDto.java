package com.example.drillix.Bha;

import java.util.List;

// Составной ответ пользователю: рекомендация + компоновка + элементы.
// Собирается из ТРЁХ таблиц (и из Equipment через элементы) — то самое
// «DTO без подходящей entity» из нашей лекции
public record BhaRecommendationResponseDto(
        Long recommendationId,
        RecommendationStatus status,
        String justtification,
        String model,
        java.time.LocalDateTime createdAt,
        Long bhaId,
        List<ElementResponse> elements
) {
    // Один элемент компоновки в ответе: позиция + данные.
    // Из справочника — полные данные позиции; свободный — freeType/freeParams
    public record ElementResponse(
            Integer orderNo,
            Long equipmentId,        // null для свободного режима
            String equipmentName,    // название из справочника или null
            String type,             // тип (из справочника или свободный)
            String freeParams,       // описание свободного элемента или null
            String comment
    ) {}

    public static BhaRecommendationResponseDto from(Bha bha,
                                                 List<BhaElement> elements,
                                                 BhaRecommendation recommendation) {
        List<ElementResponse> elementList = elements.stream()
                .map(el -> new ElementResponse(
                        el.getOrderNo(),
                        el.getEquipment() != null ? el.getEquipment().getId() : null,
                        el.getEquipment() != null ? el.getEquipment().getName() : null,
                        el.getEquipment() != null
                                ? el.getEquipment().getType().name()
                                : (el.getFreeType() != null ? el.getFreeType().name() : null),
                        el.getFreeParams(),
                        el.getComment()
                ))
                .toList();

        return new BhaRecommendationResponseDto(
                recommendation.getId(),
                recommendation.getStatus(),
                recommendation.getJusttification(),
                recommendation.getModel(),
                recommendation.getCreatedAt(),
                bha.getId(),
                elementList
        );
    }
}