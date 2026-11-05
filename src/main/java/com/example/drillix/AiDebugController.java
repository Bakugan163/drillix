package com.example.drillix;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiDebugController {

    // Spring подставит сюда значения из application.properties —
    // те самые, что реально ушли в клиент AI
    private final String baseUrl;
    private final String model;
    private final String apiKeyMasked;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // в конструктор добавить параметр PasswordEncoder passwordEncoder
    // и this.passwordEncoder = passwordEncoder;

    public AiDebugController(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model}") String model,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.passwordEncoder = passwordEncoder;
        // ключ показываем замаскированным: первые 8 символов — достаточно
        // чтобы сверить, а утекуть целиком не должно
        this.apiKeyMasked = apiKey.length() > 8
                ? apiKey.substring(0, 8) + "..." : "short-key";
    }

    @GetMapping("/api/ai-debug")
    public String debug() {
        return "base-url: " + baseUrl + "\nmodel: " + model + "\nkey: " + apiKeyMasked;
    }

        // Генерация bcrypt-хэша для пароля. ТЕХНИЧЕСКИЙ эндпоинт:
    // используется при создании пользователей. В проде его надо
    // закрыть ролями или выпилить (шаг 6), пока — для заведения первого админа
    @org.springframework.web.bind.annotation.GetMapping("/api/hash")
    public String hash(@org.springframework.web.bind.annotation.RequestParam String raw) {
        return passwordEncoder.encode(raw);
    }
}