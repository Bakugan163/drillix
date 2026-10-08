package com.example.drillix.Section;


import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SectionNotFoundException extends RuntimeException {

    public SectionNotFoundException(Long id) {
        super("Секция с id=" + id + "не найдена");
    }
    
}
