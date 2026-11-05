package com.example.drillix.bha;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BhaElementRepository extends JpaRepository<BhaElement, Long> {
    
    List<BhaElement> findAllByBhaOrderByOrderNoAsc(Bha bha);
}
