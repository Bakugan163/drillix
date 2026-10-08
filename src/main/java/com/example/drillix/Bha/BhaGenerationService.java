package com.example.drillix.Bha;

import org.springframework.stereotype.Service;

import com.example.drillix.Equipment.EquipmentRepository;
import com.example.drillix.Section.SectionRepository;

@Service 
public class BhaGenerationService {
    

    private final SectionRepository sectionRepository;
    private final EquipmentRepository equipmentRepository;
    private final BhaRepository bhaRepository;
    private final BhaElementRepository bhaElementRepository;
    private final BhaRecommendationRepository recommendationRepository;

    public BhaGenerationService(SectionRepository sectionRepository, EquipmentRepository equipmentRepository,
                                BhaRepository bhaRepository, BhaElementRepository bhaElementRepository,
                                BhaRecommendationRepository recommendationRepository) {
        this.sectionRepository=sectionRepository;
        this.equipmentRepository=equipmentRepository;
        this.bhaRepository=bhaRepository;
        this.bhaElementRepository=bhaElementRepository;
        this.recommendationRepository = recommendationRepository;
                                    
    }
}
