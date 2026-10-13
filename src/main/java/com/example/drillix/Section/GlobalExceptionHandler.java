
package com.example.drillix.Section;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleValidation(IllegalArgumentException e) {
        return ResponseEntity
               .status(HttpStatus.BAD_REQUEST)
               .body(e.getMessage());
    }

        // Ошибки генерации: нет оборудования, некорректный ответ AI и т.п.
    // Это не «сломанный сервер» (500), а «операция невозможна сейчас» —
    // возвращаем 409 Conflict: конфликт состояния. Можно и 400,
    // но 409 честнее: запрос правильный, а вот данные системы не готовы
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleGenerationState(IllegalStateException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)   // 409
                .body(e.getMessage());
    }
    
}
