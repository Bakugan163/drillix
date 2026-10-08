package com.example.drillix.Well;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;


@RestController 
@RequestMapping ("/api/wells")
public class WellController {

    private final WellService wellService;
    public WellController(WellService wellService) {
        this.wellService=wellService;
    }

    //создание скважины
    @PostMapping
    @ResponseStatus (HttpStatus.CREATED)
    public WellResponseDto create(@Valid @RequestBody WellRequestDto request) {
        return wellService.create(request);
    }

    //вернуть список всех скважин
    @GetMapping
    public List<WellResponseDto> findAll() {
        return wellService.findAll();
    }    

    //вернуть скважину по id
    @GetMapping("/{id}")
    public WellResponseDto findById(@PathVariable Long id) {
        return wellService.findById(id);
    }

    //обновление скважины
    @PutMapping("/{id}")
    public WellResponseDto update(@PathVariable Long id,
                                  @Valid @RequestBody WellRequestDto request) {
         return wellService.update(id, request);

    }

    //удаление скважины
    @DeleteMapping("/{id}")
    @ResponseStatus (HttpStatus.CREATED)
    public void delete(@PathVariable Long id) {
        wellService.delete(id);
    }

}
