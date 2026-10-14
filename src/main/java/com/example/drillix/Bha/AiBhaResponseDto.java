package com.example.drillix.Bha;

import java.util.List;

public record AiBhaResponseDto( 
    
    String justtification,

    List<AiElement> elements

    ) {
        public record AiElement(
            Long equipmentId,

            String type,

            String params
        ) {}
    }


