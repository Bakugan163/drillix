package com.example.drillix.bha;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.drillix.equipment.Equipment;
import com.example.drillix.equipment.EquipmentRepository;
import com.example.drillix.equipment.EquipmentType;
import com.example.drillix.section.Section;
import com.example.drillix.section.SectionNotFoundException;
import com.example.drillix.section.SectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service 
public class BhaGenerationService {
    
    private final SectionRepository sectionRepository;
    private final EquipmentRepository equipmentRepository;
    private final BhaRepository bhaRepository;
    private final BhaElementRepository bhaElementRepository;
    private final BhaRecommendationRepository recommendationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private static final Logger log = LoggerFactory.getLogger(BhaGenerationService.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public BhaGenerationService(SectionRepository sectionRepository,
                                EquipmentRepository equipmentRepository,
                                BhaRepository bhaRepository,
                                BhaElementRepository bhaElementRepository,
                                BhaRecommendationRepository recommendationRepository) {
        this.sectionRepository = sectionRepository;
        this.equipmentRepository = equipmentRepository;
        this.bhaRepository = bhaRepository;
        this.bhaElementRepository = bhaElementRepository;
        this.recommendationRepository = recommendationRepository;
    }

    private record AiCall(AiBhaResponseDto response, String raw) {}

    public List<Equipment> checkEquipmentAvailable(Section section) {
        List<Equipment> allEquipment = equipmentRepository.findAll();
        
        boolean hasBit = allEquipment.stream()
                .anyMatch(e -> e.getType() == EquipmentType.BIT 
                            && e.getDiameter().compareTo(section.getBitDiametr()) == 0);

        if (!hasBit) {
            throw new IllegalStateException(
                "В справочнике нет долота диаметром " + section.getBitDiametr()
                            + " мм — пополните справочник и повторите генерацию");
        }
        
        // Берем оборудование не больше диаметра долота, сортируем по убыванию
        // Ограничиваем 100 позициями
        return allEquipment.stream()
                .filter(e -> e.getDiameter().compareTo(section.getBitDiametr()) <= 0)
                .sorted((e1, e2) -> e2.getDiameter().compareTo(e1.getDiameter()))
                .limit(100)
                .toList();
    }

    private String currentModelName() {
        return "deepseek-v4-pro"; 
    }

    @Transactional
    public BhaRecommendationResponseDto generate(Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new SectionNotFoundException(sectionId));

        List<Equipment> available = checkEquipmentAvailable(section);
        AiCall aiCall = callAi(section, available);
        validateResponse(aiCall.response(), available, section);

        return saveAll(section, available, aiCall.response(), aiCall.raw(), currentModelName());
    }

    private AiCall callAi(Section section, List<Equipment> available) {
        // 1. Параметры секции и скважины (контекст)
        String wellType = section.getWell().getWellType().name();
        
        String sectionInfo = """
                ТИП СКВАЖИНЫ: %s
                ТИП СЕКЦИИ: %s
                Интервал: %s–%s м
                Ø долота: %s мм
                Траектория: %s
                Зенит: %s
                Условия: %s
                """.formatted(
                wellType,
                section.getType(), section.getDepthFrom(), section.getDepthTo(),
                section.getBitDiametr(), section.getTrajectoryTask(),
                section.getMaxZenithAngle() != null ? section.getMaxZenithAngle() : "—",
                section.getDrillingCondition() != null ? section.getDrillingCondition() : "—");

        // 2. Справочник (компактно)
        String catalogInfo = available.stream()
                .map(e -> "#%d | %s | %s | %s мм".formatted(
                        e.getId(), e.getType(), e.getName(), e.getDiameter()))
                .collect(Collectors.joining("\n"));

        // 3. Системный промпт (с ИНЖЕНЕРНЫМИ ПРАВИЛАМИ)
        String systemPrompt = """
    Ты — опытный технолог по бурению. Твоя задача — подобрать КНБК для секции, используя СПРАВОЧНИК.
    
    ИНЖЕНЕРНЫЕ ПРАВИЛА (СТРОГО СОБЛЮДАТЬ):
    1. Направление (DIRECTION) и Кондуктор (CONDUCTOR): 
       - ЗАПРЕЩЕНО использовать: НУБТ (NMDC), LWD, РУС (RSS).
       - MWD на кондукторе разрешен ТОЛЬКО с электромагнитным каналом связи (ЭМ).
    2. ВЗД (MOTOR) и РУС (RSS) не могут быть в одной КНБК. Выбери что-то одно.
    3. На всех секциях, КРОМЕ Направления (DIRECTION), обязательно должны быть: 
       - Двигатель (ВЗД или РУС).
       - Калибраторы (CALIPER).
    4. Если скважина НЕ вертикальная (траектория не VERTICAL), у ВЗД обязательно должен быть указан угол перекоса (напиши это в params или выбери подходящий из базы).
    5. Диаметр оборудования (ВЗД, УБТ, телеметрия) обычно МЕНЬШЕ диаметра долота (например, для долота 393.7 мм подойдет ВЗД 203 мм).
      6. Формат ответа — строго JSON-объект.
       ВНИМАНИЕ: Если элемент берется из базы (equipmentId), НЕ ЗАПОЛНЯЙ поля type и params (оставь их null или не пиши вообще).
       Если элемента нет в базе, НЕ ЗАПИСЫВАЙ equipmentId (оставь null), а заполни ТОЛЬКО type (английский код) и params (русский текст).
       
       Пример:
       {
         "justtification": "...",
         "elements": [
           {"equipmentId": 12}, 
           {"type": "MOTOR", "params": "ВЗД 203 мм, нет в базе"}
         ]
       }
    }
    """;

        String userPrompt = """
                ПОДБЕРИ КНБК.
                
                СЕКЦИЯ:
                %s
                
                ДОСТУПНОЕ ОБОРУДОВАНИЕ:
                %s
                """.formatted(sectionInfo, catalogInfo);

        try {
            String systemJson = objectMapper.writeValueAsString(systemPrompt);
            String userJson = objectMapper.writeValueAsString(userPrompt);

            String requestBody = """
                    {
                      "model": "deepseek-v4-pro",
                      "messages": [
                        {"role": "system", "content": %s},
                        {"role": "user", "content": %s}
                      ],
                      "temperature": 0.0,
                      "max_tokens": 1500,
                      "response_format": {"type": "json_object"}
                    }
                    """.formatted(systemJson, userJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://openai.bothub.chat/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("Ошибка AI " + response.statusCode() + ": " + response.body());
            }

            String content = objectMapper.readTree(response.body())
                    .path("choices").get(0).path("message").path("content").asText();

            String json = content.replace("```json", "").replace("```", "").trim();
            AiBhaResponseDto parsed = objectMapper.readValue(json, AiBhaResponseDto.class);
            
            return new AiCall(parsed, content);

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка вызова AI: " + e.getMessage(), e);
        }
    }

        private void validateResponse(AiBhaResponseDto response, List<Equipment> available, Section section) {
        if (response.elements() == null || response.elements().isEmpty()) {
            throw new IllegalStateException("AI не вернула ни одного элемента компоновки");
        }

        boolean hasMotor = false;
        boolean hasRss = false;
        boolean hasBit = false;

        for (AiBhaResponseDto.AiElement el : response.elements()) {
            // Приводим строки к нормальному виду (убираем пустые строки "")
            String typeStr = el.type() != null ? el.type().trim() : null;
            String paramsStr = el.params() != null ? el.params().trim() : null;
            
            boolean fromCatalog = el.equipmentId() != null;
            boolean free = (typeStr != null && !typeStr.isEmpty()) && (paramsStr != null && !paramsStr.isEmpty());

            // Если заполнено И то, И другое - оставляем только ID (или можно выкинуть ошибку)
            if (fromCatalog && free) {
                log.warn("AI прислала и ID, и текст для одного элемента. Приоритет отдан ID: {}", el.equipmentId());
                // В данном случае мы не падаем, а просто игнорируем текст, так как ID важнее
                typeStr = null;
                paramsStr = null;
                free = false;
            }

            // Если не заполнено ни то, ни другое - падаем с понятной ошибкой
            if (!fromCatalog && !free) {
                throw new IllegalStateException("Ошибка структуры данных от ИИ. Элемент должен иметь ЛИБО equipmentId, ЛИБО заполненные type и params. Получено: equipmentId=" 
                    + el.equipmentId() + ", type=" + typeStr + ", params=" + paramsStr);
            }

            // Определяем тип элемента для проверки
            EquipmentType type = null;
            if (fromCatalog) {
                Equipment eq = available.stream().filter(e -> e.getId().equals(el.equipmentId())).findFirst().orElse(null);
                if (eq != null) type = eq.getType();
            } else {
                type = parseEquipmentType(typeStr);
            }

            if (type == EquipmentType.MOTOR) hasMotor = true;
            if (type == EquipmentType.RSS) hasRss = true;
            if (type == EquipmentType.BIT) hasBit = true;

            // ИНЖЕНЕРНАЯ ВАЛИДАЦИЯ
            if ((section.getType().name().equals("DIRECTION") || section.getType().name().equals("CONDUCTOR"))) {
                if (type == EquipmentType.NMDC || type == EquipmentType.LWD || type == EquipmentType.RSS) {
                    throw new IllegalStateException("Ошибка AI: для секции " + section.getType() + " запрещено использовать " + type);
                }
            }
        }

        if (hasMotor && hasRss) {
            throw new IllegalStateException("Ошибка AI: ВЗД (MOTOR) и РУС (RSS) не могут быть в одной компоновке.");
        }

        if (!hasBit) {
            throw new IllegalStateException("В компоновке отсутствует ДОЛОТО (BIT).");
        }

        AiBhaResponseDto.AiElement first = response.elements().get(0);
        if (first.equipmentId() == null) {
            throw new IllegalStateException("Долото должно быть на позиции 1 и браться из справочника");
        }

        Equipment bit = available.stream()
                .filter(e -> e.getId().equals(first.equipmentId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("AI указала несуществующий equipmentId=" + first.equipmentId()));

        if (bit.getType() != EquipmentType.BIT) {
            throw new IllegalStateException("На позиции 1 должен быть BIT, а AI поставила " + bit.getType());
        }

        for (AiBhaResponseDto.AiElement el : response.elements()) {
            if (el.equipmentId() != null) {
                boolean exists = available.stream().anyMatch(e -> e.getId().equals(el.equipmentId()));
                if (!exists) {
                    throw new IllegalStateException("AI указала equipmentId=" + el.equipmentId() + ", которого нет в справочнике.");
                }
            }
        }
    }

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
            
            if (el.equipmentId() != null) {
                Equipment equipment = available.stream()
                        .filter(e -> e.getId().equals(el.equipmentId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Позиция " + el.equipmentId() + " не найдена"));
                element.setEquipment(equipment);
            } else {
                EquipmentType requestedType = parseEquipmentType(el.type());
                
                // Автоподстановка: если AI написала текст, но в базе есть такой тип
                // Ищем подходящий по диаметру (не больше долота)
                Equipment matchInDb = available.stream()
                        .filter(e -> e.getType() == requestedType)
                        .filter(e -> e.getDiameter().compareTo(section.getBitDiametr()) <= 0)
                        .sorted((e1, e2) -> e2.getDiameter().compareTo(e1.getDiameter()))
                        .findFirst()
                        .orElse(null);

                if (matchInDb != null) {
                    log.warn("AI написала текст для {}. Автоматически подставляем ID: {}", el.type(), matchInDb.getId());
                    element.setEquipment(matchInDb);
                } else {
                    element.setFreeType(requestedType);
                    element.setFreeParams(el.params());
                }
            }
            savedElements.add(bhaElementRepository.save(element));
        }

                BhaRecommendation recommendation = new BhaRecommendation();
        recommendation.setSection(section);
        recommendation.setBha(bha);
        recommendation.setStatus(RecommendationStatus.DRAFT);
        
        // Защита от пустого обоснования
        String just = aiResponse.justtification();
        if (just == null || just.trim().isEmpty()) {
            just = "Обоснование не предоставлено нейросетью.";
        }
        recommendation.setJusttification(just);
        
        recommendation.setModel(modelName);
        recommendation.setRawResponse(rawAiResponse);

        recommendation = recommendationRepository.save(recommendation);

        return BhaRecommendationResponseDto.from(bha, savedElements, recommendation);
    } 

    // ... ОСТАЛЬНЫЕ МЕТОДЫ (getHistory, approve, reject, changeStatus, editBha) ОСТАЮТСЯ БЕЗ ИЗМЕНЕНИЙ ...
    // В методе editBha не забудь передать section в validateResponse!
    
    @Transactional(readOnly=true)
    public List<BhaRecommendationResponseDto> getHistory(Long sectionId) {
        Section section = sectionRepository.findById(sectionId).orElseThrow(() -> new SectionNotFoundException(sectionId));
        return recommendationRepository.findAllBySectionOrderByCreatedAtDesc(section).stream()
                .map(rec -> {
                    List<BhaElement> elements = bhaElementRepository.findAllByBhaOrderByOrderNoAsc(rec.getBha());
                    return BhaRecommendationResponseDto.from(rec.getBha(), elements, rec);
                }).toList();
    }

    @Transactional
    public BhaRecommendationResponseDto approve(Long sectionId, Long recommendationId) {
        return changeStatus(sectionId, recommendationId, RecommendationStatus.APPROVED);
    }

    @Transactional
    public BhaRecommendationResponseDto reject(Long sectionId, Long recommendationId) {
        return changeStatus(sectionId, recommendationId, RecommendationStatus.REJECTED);
    }

    private BhaRecommendationResponseDto changeStatus(Long sectionId, Long recommendationId, RecommendationStatus newStatus) {
        Section section = sectionRepository.findById(sectionId).orElseThrow(() -> new SectionNotFoundException(sectionId));
        BhaRecommendation recommendation = recommendationRepository.findById(recommendationId)
                .filter(rec -> rec.getSection().getId().equals(section.getId()))
                .orElseThrow(() -> new IllegalStateException("Рекомендация не найдена"));

        recommendation.setStatus(newStatus);
        recommendation = recommendationRepository.save(recommendation);

        List<BhaElement> elements = bhaElementRepository.findAllByBhaOrderByOrderNoAsc(recommendation.getBha());
        return BhaRecommendationResponseDto.from(recommendation.getBha(), elements, recommendation);
    }

    @Transactional
    public BhaRecommendationResponseDto editBha(Long sectionId, Long recommendationId, BhaEditRequestDto request) {
        Section section = sectionRepository.findById(sectionId).orElseThrow(() -> new SectionNotFoundException(sectionId));
        BhaRecommendation recommendation = recommendationRepository.findById(recommendationId)
                .filter(rec -> rec.getSection().getId().equals(section.getId()))
                .orElseThrow(() -> new IllegalStateException("Рекомендация не найдена в секции"));

        if (recommendation.getStatus() != RecommendationStatus.DRAFT) {
            throw new IllegalStateException("Редактировать можно только черновик (DRAFT)");
        }

        List<Equipment> available = checkEquipmentAvailable(section);

        List<AiBhaResponseDto.AiElement> forValidation = request.elements().stream()
                .map(el -> new AiBhaResponseDto.AiElement(el.equipmentId(), el.type(), el.params()))
                .toList();
        // ПЕРЕДАЕМ SECTION В ВАЛИДАЦИЮ
        validateResponse(new AiBhaResponseDto(null, forValidation), available, section);

        List<BhaElement> oldElements = bhaElementRepository.findAllByBhaOrderByOrderNoAsc(recommendation.getBha());
        bhaElementRepository.deleteAll(oldElements);

        List<BhaElement> savedElements = new java.util.ArrayList<>();
        int order = 1;
        for (BhaEditRequestDto.ElementDto el : request.elements()) {
            BhaElement element = new BhaElement();
            element.setBha(recommendation.getBha());
            element.setOrderNo(order++);
            element.setComment(el.comment());
            
            if (el.equipmentId() != null) {
                Equipment equipment = available.stream()
                        .filter(e -> e.getId().equals(el.equipmentId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Позиция " + el.equipmentId() + " не найдена"));
                element.setEquipment(equipment);
            } else {
                element.setFreeType(parseEquipmentType(el.type()));
                element.setFreeParams(el.params());
            }
            savedElements.add(bhaElementRepository.save(element));
        }

        return BhaRecommendationResponseDto.from(recommendation.getBha(), savedElements, recommendation);
    }

    private EquipmentType parseEquipmentType(String typeStr) {
        if (typeStr == null) return null;
        String upper = typeStr.trim().toUpperCase();
        switch (upper) {
            case "ВЗД": return EquipmentType.MOTOR;
            case "УБТ": return EquipmentType.DC;
            case "НУБТ": return EquipmentType.NMDC;
            case "ДОЛОТО": return EquipmentType.BIT;
            case "КАЛИБРАТОР": return EquipmentType.CALIPER;
            case "СТАБИЛИЗАТОР": return EquipmentType.STABILIZER;
            case "ПЕРЕВОДНИК": return EquipmentType.SUB;
            case "ТЕЛЕСИСТЕМА": return EquipmentType.MWD;
            default:
                try {
                    return EquipmentType.valueOf(upper);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("AI вернула неизвестный тип оборудования: " + typeStr);
                }
        }
    }

        @Transactional
    public void deleteRecommendation(Long sectionId, Long recommendationId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new SectionNotFoundException(sectionId));

        BhaRecommendation recommendation = recommendationRepository.findById(recommendationId)
                .filter(rec -> rec.getSection().getId().equals(section.getId()))
                .orElseThrow(() -> new IllegalStateException("Рекомендация не найдена в этой секции"));

        recommendationRepository.delete(recommendation);
    }
}