package com.example.drillix.Section;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.drillix.Well.Well;


public interface SectionRepository extends JpaRepository<Section, Long> {
    
    List<Section> findAllByWellOrderByDepthFromAsc(Well well);

    @Query("""
            SELECT s FROM Section s
            WHERE s.well = well
               AND (
                    (s.depthFrom <= :to AND s.depthTo >= :from)
               )
            """)

List<Section> findOverlapping(@Param("well") Well well,
                              @Param("from") Integer from,
                              @Param("to") Integer to);            



}
