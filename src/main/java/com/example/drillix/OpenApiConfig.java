package com.example.drillix;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Конфигурация OpenAPI-документации (Swagger).
// SecurityScheme — объявление схемы аутентификации для сваггера:
// «этот API защищён HTTP Basic». После этого Swagger понимает:
// показывает кнопку Authorize с полноценным входом и Logout
@Configuration
@SecurityScheme(
        name = "basicAuth",                    // имя схемы (ссылка снизу)
        type = SecuritySchemeType.HTTP,        // схема уровня HTTP
        scheme = "basic"                       // именно Basic Auth
)
public class OpenApiConfig {
    // Класс-объявление, тело пустое: аннотации делают всё сами
}