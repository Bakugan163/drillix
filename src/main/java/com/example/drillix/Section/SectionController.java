package com.example.drillix.Section;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@RestController 
@RequestMapping ("/api/wells/{wellId}/sections")
public class SectionController {

    private final SectionService sectionService;
    public SectionController(SectionService sectionService) {
        this.sectionService=sectionService;
    }

    //создание секции
    @PostMapping 
    @ResponseStatus (HttpStatus.CREATED)
    public SectionResponseDto create(@PathVariable Long wellId,
                                  @Valid @RequestBody SectionRequestDto request) {
      return sectionService.create(wellId, request);
    }

    //отдать все секции определеной скважины
    @GetMapping 
    public List<SectionResponseDto> findAll(@PathVariable Long wellId) {
        return sectionService.findAllByWell(wellId);
        
    }

    //достать секцию по id
    @GetMapping ("/{sectionId}")
    public SectionResponseDto findById(@PathVariable Long wellId,
                                       @PathVariable Long sectionId) {
        return sectionService.findById(wellId, sectionId);
    }

    //обновление секции
    @PutMapping ("/{sectionId}")
    public SectionResponseDto update(@PathVariable Long wellId,
                                     @PathVariable Long sectionId,
                                     @Valid @RequestBody SectionRequestDto request) {
    return sectionService.update(wellId, sectionId, request);
    }

    //удаление
    @DeleteMapping ("/{sectionId}")
    @ResponseStatus (HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long wellId,
                       @PathVariable Long sectionId) {
    sectionService.delete(wellId, sectionId);

    }
    
}
