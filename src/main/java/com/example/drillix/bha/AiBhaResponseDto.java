package com.example.drillix.bha;

import java.util.List;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
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


