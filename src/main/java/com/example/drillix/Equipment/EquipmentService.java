package com.example.drillix.Equipment;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.networknt.schema.Result;

@Service 
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository=equipmentRepository;
    }

    public EquipmentResponseDto crate(EquipmentRequestDto request) {
        Equipment equipment = new Equipment();
        copyFields(equipment, request);
        return EquipmentResponseDto.from(equipmentRepository.save(equipment));
    }

    public List<EquipmentResponseDto> findAll(EquipmentType type, BigDecimal diameter) {
            List<Equipment> result;
        if (type !=null && diameter != null) {
            result = equipmentRepository.findByTypeAndDiameter(type, diameter);
        } else if (diameter != null) {
            result = equipmentRepository.findAllByDiameterOrderByTypeAscNameAsc(diameter);
        } else if (type != null) {
            result = equipmentRepository.findByType(type); 
        } else {
            result = equipmentRepository.findAll();
        }
        return result.stream().map(EquipmentResponseDto::from).toList();
    }

    public EquipmentResponseDto findById(Long id) {
        return EquipmentResponseDto.from(getOrThrow(id));
    }

    public EquipmentResponseDto update(Long id, EquipmentRequestDto request) {
        Equipment equipment = getOrThrow(id);
        copyFields(equipment, request);
        return EquipmentResponseDto.from(equipmentRepository.save(equipment));
    }

    public void delete(Long id) {
        equipmentRepository.delete(getOrThrow(id));
    }

    private Equipment getOrThrow(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(()-> new EquipmentNotFoundException(id));
    }

    private void copyFields(Equipment equipment, EquipmentRequestDto request) {
        equipment.setType(request.type());
        equipment.setName(request.name());
        equipment.setDiameter(request.diameter());
        equipment.setDescription(request.description());
    }
    
}
