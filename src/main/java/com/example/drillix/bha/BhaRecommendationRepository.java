package com.example.drillix.bha;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.drillix.section.Section;

public interface BhaRecommendationRepository extends JpaRepository<BhaRecommendation, Long> {
    
    List<BhaRecommendation> findAllBySectionOrderByCreatedAtDesc(Section section);
}
