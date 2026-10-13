package com.example.drillix.Bha;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.drillix.Equipment.Equipment;
import com.example.drillix.Equipment.EquipmentRepository;
import com.example.drillix.Equipment.EquipmentType;
import com.example.drillix.Section.Section;
import com.example.drillix.Section.SectionNotFoundException;
import com.example.drillix.Section.SectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.transaction.annotation.Transactional;

@Service 
public class BhaGenerationService {
    

    private final SectionRepository sectionRepository;
    private final EquipmentRepository equipmentRepository;
    private final BhaRepository bhaRepository;
    private final BhaElementRepository bhaElementRepository;
    private final BhaRecommendationRepository recommendationRepository;

     // НОВОЕ: строитель клиента Spring AI. Spring сам его подсунет —
    // стартер model-openai создаёт ChatClient.Builder автоматически
    private final ChatClient.Builder chatClientBuilder;

    // НОВОЕ: сам клиент — соберём один раз в конструкторе
    private ChatClient chatClient;

    // Готовый парсер JSON — тоже один раз, он без состояния
    private final ObjectMapper objectMapper = new ObjectMapper();



    public BhaGenerationService(SectionRepository sectionRepository,
                                EquipmentRepository equipmentRepository,
                                BhaRepository bhaRepository,
                                BhaElementRepository bhaElementRepository,
                                BhaRecommendationRepository recommendationRepository,
                                ChatClient.Builder chatClientBuilder) {
        this.sectionRepository=sectionRepository;
        this.equipmentRepository=equipmentRepository;
        this.bhaRepository=bhaRepository;
        this.bhaElementRepository=bhaElementRepository;
        this.recommendationRepository = recommendationRepository;
        this.chatClientBuilder = chatClientBuilder;
        this.chatClient = chatClientBuilder.build();                         
    }

    // Обёртка результата вызова AI: разобранный ответ + СЫРОЙ текст.
    // Сырой нужен для аудита (raw_ai_response) — храним бит в бит
    private record AiCall(AiBhaResponseDto response, String raw) {}

    public List<Equipment> checkEquipmentAvailable(Section section) {
        List<Equipment> available = equipmentRepository
                        .findAllByDiameterOrderByTypeAscNameAsc(section.getBitDiametr());

        boolean hasBit = available.stream()
                .anyMatch(e->e.getType() == EquipmentType.BIT);

        if (!hasBit) {
            throw new IllegalStateException(
                "В справочнике нет долота диаметром " + section.getBitDiametr()
                            + " мм — пополните справочник и повторите генерацию");
        }
        return available;
     }

     private String currentModelName() {
        return "nvidia/nemotron-3-ultra-550b-a55b:free";
    }

     @Transactional
     public BhaRecommendationResponseDto generate(Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                    .orElseThrow(()->new SectionNotFoundException(sectionId));
     

     List<Equipment> available = checkEquipmentAvailable(section);

     AiCall aiCall = callAi(section, available);

     validateResponse(aiCall.response(), available);

     return saveAll(section, available, aiCall.response(), aiCall.raw(), currentModelName());

    }

    private AiCall callAi(Section section, List<Equipment> available) {
        // Часть 1: параметры секции
        String sectionInfo = """
                Тип секции: %s
                Интервал: %s–%s м
                Диаметр долота: %s мм
                Задача по траектории: %s
                Максимальный зенитный угол: %s
                Условия бурения: %s
                """.formatted(
                section.getType(),
                section.getDepthFrom(), section.getDepthTo(),
                section.getBitDiametr(),
                section.getTrajectoryTask(),
                section.getMaxZenithAngle() != null ? section.getMaxZenithAngle() : "не задан",
                section.getDrillingCondition() != null ? section.getDrillingCondition() : "не заданы");

        // Часть 2: справочник — сгруппированный по типам список с id
        String catalogInfo = available.stream()
                .map(e -> "- #%d [%s] %s (%s мм)".formatted(
                        e.getId(), e.getType(), e.getName(), e.getDiameter()))
                .collect(java.util.stream.Collectors.joining("\n"));

        // Часть 3: инструкция + формат ответа
        String systemPrompt = """
                Ты — опытный технолог по бурению. Твоя задача — подобрать компоновку
                низа бурильной колонны (КНБК) для секции скважины.

                ПРАВИЛА:
                1. Элементы компоновки перечисляй от долота вверх.
                2. Первым элементом обязательно должно быть ДОЛОТО из справочника.
                3. Для каждого элемента используй ЛИБО equipmentId существующей
                   позиции справочника, ЛИБО (если подходящего оборудования нет)
                   свободное описание: type + params.
                4. Не выдумывай equipmentId — используй только id из списка.
                5. Ответ дай СТРОГО в JSON, без пояснений до и после:
                {
                  "justtification": "текстовое обоснование выбора",
                  "elements": [
                    {"equipmentId": 12, "type": null, "params": null},
                    {"equipmentId": null, "type": "MWD", "params": "описание"}
                  ]
                }
                """;

        String userPrompt = "СЕКЦИЯ:\n" + sectionInfo + "\nДОСТУПНОЕ ОБОРУДОВАНИЕ:\n" + catalogInfo;

        // === ВЫЗОВ МОДЕЛИ ===

        String rawResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        // rawResponse — то, что ответила модель, строкой.
        // Это и есть наш raw_ai_response для аудита!
        System.out.println("RAW AI RESPONSE: " + rawResponse);

        // === ПАРСИНГ: JSON-строка → наш DTO ===

        // Модель может обернуть ответ в ```json ... ``` — чистим
        String json = rawResponse
                .replace("```json", "")
                .replace("```", "")
                .trim();

        try {
            AiBhaResponseDto parsed = objectMapper.readValue(json, AiBhaResponseDto.class);
            return new AiCall(parsed, rawResponse);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Ответ модели не удалось разобрать как JSON: " + rawResponse);
        }
    }
    

    // === ПРОВЕРКИ ОТВЕТА AI (FR-3.6) — вся защита здесь ===

    private void validateResponse(AiBhaResponseDto response, List<Equipment> available) {
        if (response.elements()==null || response.elements().isEmpty()) {
            throw new IllegalStateException("AI не вернула ни одного элемента компоновки");
        }


        for (AiBhaResponseDto.AiElement el : response.elements()) {
            boolean fromCatalog = el.equipmentId() != null;
            boolean free = el.type() != null && el.params() != null;

            if (fromCatalog==free) {
                throw new IllegalStateException(
                    "Элемент должен иметь ЛИБО equipmentId, ЛИБО type+params. "
                                + "Получено: equipmentId=" + el.equipmentId()
                                + ", type=" + el.type()
                );
            }
        }

        AiBhaResponseDto.AiElement first = response.elements().get(0);
        if (first.equipmentId()==null) {
            throw new IllegalStateException(
                    "Долото должно быть на позиции 1 и браться из справочника");
        }

        Equipment bit = available.stream()
                        .filter(e->e.getId().equals(first.equipmentId()))
                        .findFirst()
                        .orElseThrow(()->new IllegalStateException(
                            "AI указала несуществующий equipmentId=" + first.equipmentId()));

        if (bit.getType() != EquipmentType.BIT) {
            throw new IllegalStateException(
                 "На позиции 1 должен быть BIT, а AI поставила " + bit.getType());
        }

        for (AiBhaResponseDto.AiElement el : response.elements()) {
            if (el.equipmentId() != null) {
                boolean exists = available.stream()
                        .anyMatch(e->e.getId().equals(el.equipmentId()));
                if(!exists) {
                    throw new IllegalStateException(
                        "AI указала equipmentId=" + el.equipmentId()
                                    + ", которого нет в справочнике для этого диаметра"
                    );
                }
            }
        }
                        
    }

     // === СОХРАНЕНИЕ: три таблицы за один запрос ===

     private BhaRecommendationResponseDto saveAll(Section section,
                                                  List<Equipment> available,  
                                                  AiBhaResponseDto aiResponse,
                                                  String rawAiResponse,
                                                  String modelName) {
        
        
            Bha bha = new Bha();
            bha = bhaRepository.save(bha);

            List<BhaElement> savedElements = new java.util.ArrayList<>();
            int order = 1;
            for (AiBhaResponseDto.AiElement el : aiResponse.elements()) {
                BhaElement element = new BhaElement();
                element.setBha(bha);
                element.setOrderNo(order++);
                if(el.equipmentId()!=null) {
                    Equipment equipment = available.stream()
                                .filter(e->e.getId().equals(el.equipmentId()))
                                .findFirst()
                                .orElseThrow(()-> new IllegalStateException(
                                    "Позиция " + el.equipmentId() + " не найдена"));
                        
                    element.setEquipment(equipment);
                } else {
                    element.setFreeType(EquipmentType.valueOf(el.type()));
                    element.setFreeParams(el.params());
                }
                savedElements.add(bhaElementRepository.save(element));
            }

        BhaRecommendation recommendation = new BhaRecommendation();
        recommendation.setSection(section);
        recommendation.setBha(bha);
        recommendation.setStatus(RecommendationStatus.DRAFT);
        recommendation.setJusttification(aiResponse.justification());
        recommendation.setModel(modelName);
        recommendation.setRawResponse(rawAiResponse);
        recommendation = recommendationRepository.save(recommendation);

        return BhaRecommendationResponseDto.from(bha, savedElements, recommendation);

    } 


    private Equipment availableById(List<Equipment> available, Long id) {
        return equipmentRepository.findById(id)
                        .orElseThrow(()-> new IllegalStateException(
                        "Позиция справочника " + id + " исчезла"));
    }

        // История рекомендаций секции (FR-3.3)
    public List<BhaRecommendationResponseDto> getHistory(Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new SectionNotFoundException(sectionId));

        return recommendationRepository
                .findAllBySectionOrderByCreatedAtDesc(section).stream()
                .map(rec -> {
                    // Достаём элементы компоновки этой рекомендации
                    List<BhaElement> elements = bhaElementRepository
                            .findAllByBhaOrderByOrderNoAsc(rec.getBha());
                    return BhaRecommendationResponseDto.from(rec.getBha(), elements, rec);
                })
                .toList();
    }
    
}
