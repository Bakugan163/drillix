package com.example.drillix.equipment;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    
    List<Equipment> findByTypeAndDiameter(EquipmentType type, BigDecimal diametr);
    
    List<Equipment> findAllByDiameterOrderByTypeAscNameAsc(BigDecimal diameter);

    List<Equipment> findByType(EquipmentType type);
}
