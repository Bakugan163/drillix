package com.example.drillix.Equipment;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController 
@RequestMapping ("/api/equipment")
public class EquipmentController {
    
    private final EquipmentService equipmentService;
    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;

    }

    @PostMapping 
    @ResponseStatus 
    (HttpStatus.CREATED)
    public EquipmentResponseDto create(@Valid @RequestBody EquipmentRequestDto request) {
        return equipmentService.crate(request);
    }

    @GetMapping 
    public List<EquipmentResponseDto> findAll(
               @RequestParam(required=false) EquipmentType type,
               @RequestParam (required=false) BigDecimal diameter) {

        return equipmentService.findAll(type, diameter);         
    }

    @PutMapping("/{id}")
    public EquipmentResponseDto update(@PathVariable Long id,
                                       @Valid @RequestBody EquipmentRequestDto request) {

        return equipmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus (HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        equipmentService.delete(id);
    }
}
