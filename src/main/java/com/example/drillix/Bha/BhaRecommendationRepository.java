package com.example.drillix.Bha;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.drillix.Section.Section;

public interface BhaRecommendationRepository extends JpaRepository<BhaRecommendation, Long> {
    
    List<BhaRecommendation> findAllBySectionOrderByCreatedAtDesc(Section section);
}
