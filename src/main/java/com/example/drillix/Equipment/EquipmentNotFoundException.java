package com.example.drillix.Equipment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus (HttpStatus.NOT_FOUND)
public class EquipmentNotFoundException extends RuntimeException {

    public EquipmentNotFoundException(Long id) {
        super("Позиция справочника с id=" + id + "не найдена");
    }
    
}
