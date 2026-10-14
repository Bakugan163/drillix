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

    public AiDebugController(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model}") String model,
            @Value("${spring.ai.openai.api-key}") String apiKey) {
        this.baseUrl = baseUrl;
        this.model = model;
        // ключ показываем замаскированным: первые 8 символов — достаточно
        // чтобы сверить, а утекуть целиком не должно
        this.apiKeyMasked = apiKey.length() > 8
                ? apiKey.substring(0, 8) + "..." : "short-key";
    }

    @GetMapping("/api/ai-debug")
    public String debug() {
        return "base-url: " + baseUrl + "\nmodel: " + model + "\nkey: " + apiKeyMasked;
    }
}