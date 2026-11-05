package com.example.drillix.bha;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController 
@RequestMapping("/api/wells/{wellId}/sections/{sectionId}/recommendations")
public class BhaGenerationController {
    

    private final BhaGenerationService bhaGenerationService;
    private final BhaExportService exportService;
    public BhaGenerationController(BhaGenerationService bhaGenerationService, BhaExportService exportService) {
        this.bhaGenerationService=bhaGenerationService;
        this.exportService=exportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BhaRecommendationResponseDto generate(@PathVariable Long wellId,
                                                 @PathVariable Long sectionId) {
        return bhaGenerationService.generate(sectionId);
    }

    // История подборов секции (FR-3.3): все рекомендации,
    // свежие первыми (сортировка в репозитории)
    @GetMapping
    public List<BhaRecommendationResponseDto> history(@PathVariable Long wellId,
                                                      @PathVariable Long sectionId) {
        return bhaGenerationService.getHistory(sectionId);
    }

        // Согласовать рекомендацию: DRAFT → APPROVED
    @PostMapping("/{recommendationId}/approve")
    public BhaRecommendationResponseDto approve(@PathVariable Long wellId,
                                                @PathVariable Long sectionId,
                                                @PathVariable Long recommendationId) {
        return bhaGenerationService.approve(sectionId, recommendationId);
    }

    // Отклонить рекомендацию: DRAFT → REJECTED
    @PostMapping("/{recommendationId}/reject")
    public BhaRecommendationResponseDto reject(@PathVariable Long wellId,
                                                @PathVariable Long sectionId,
                                                @PathVariable Long recommendationId) {
        return bhaGenerationService.reject(sectionId, recommendationId);
    }

        // Ручное редактирование компоновки DRAFT-рекомендации (FR-4)
    @PutMapping("/{recommendationId}/bha")
    public BhaRecommendationResponseDto editBha(@PathVariable Long wellId,
                                                @PathVariable Long sectionId,
                                                @PathVariable Long recommendationId,
                                                @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody BhaEditRequestDto request) {
        return bhaGenerationService.editBha(sectionId, recommendationId, request);
    }

        // Экспорт рекомендации в Excel (FR-4). GET — скачивание файла:
    // возвращает не JSON, а .xlsx с правильными заголовками
    @GetMapping("/{recommendationId}/export")
    public org.springframework.http.ResponseEntity<byte[]> export(
            @PathVariable Long wellId,
            @PathVariable Long sectionId,
            @PathVariable Long recommendationId) {

        byte[] excel = exportService.exportToExcel(recommendationId);

        return org.springframework.http.ResponseEntity.ok()
                // Тип содержимого: Excel-файл
                .header("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                // Имя файла при скачивании
                .header("Content-Disposition",
                        "attachment; filename=bha-recommendation-" + recommendationId + ".xlsx")
                .body(excel);
    }

        @org.springframework.web.bind.annotation.DeleteMapping("/{recommendationId}")
    public org.springframework.http.ResponseEntity<Void> deleteRecommendation(
            @PathVariable Long wellId,
            @PathVariable Long sectionId,
            @PathVariable Long recommendationId) {
        
        // Вызываем метод удаления из сервиса
        bhaGenerationService.deleteRecommendation(sectionId, recommendationId);
        
        return org.springframework.http.ResponseEntity.noContent().build(); // 204 No Content
    }
}
