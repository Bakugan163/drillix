package com.example.drillix.bha;


import jakarta.validation.constraints.NotNull;

import java.util.List;

// Запрос на редактирование компоновки (FR-4, вариант «целиком»):
// клиент присылает ПОЛНЫЙ новый состав, сервер заменяет им старый.
// Правила элементов — те же, что при генерации (FR-3.6)
public record BhaEditRequestDto(

        // Полный список элементов от долота вверх, ровно в нужном порядке.
        // Пустой список запрещён — компоновка без элементов не бывает
        @NotNull(message = "Передайте список элементов")
        List<ElementDto> elements
) {
    // Один элемент в редактировании — два режима, как у BhaElement:
    public record ElementDto(

            // Режим 1: ссылка на справочник
            Long equipmentId,

            // Режим 2: свободное описание
            String type,
            String params,

            // Комментарий технолога — единственное поле, которое
            // редактирование добавляет СВОБОЛЬНО (раньше только null был!)
            String comment
    ) {}
}