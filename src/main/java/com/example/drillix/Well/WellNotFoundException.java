package com.example.drillix.Well;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class WellNotFoundException extends RuntimeException {
    
    public WellNotFoundException(Long id) {
        super("Скважина с id=" + id + "не найдена");
    }
}
