package com.example.drillix.Bha;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController 
@RequestMapping("/api/wells/{wellId}/sections/{sectionId}/recommendations")
public class BhaGenerationController {
    

    private final BhaGenerationService bhaGenerationService;
    public BhaGenerationController(BhaGenerationService bhaGenerationService) {
        this.bhaGenerationService=bhaGenerationService;
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
}
