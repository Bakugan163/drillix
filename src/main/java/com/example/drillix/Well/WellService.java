package com.example.drillix.Well;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WellService {

    private final WellRepository wellRepository;

    public WellService(WellRepository wellRepository) {
        this.wellRepository=wellRepository;
    }

    public WellResponseDto create(WellRequestDto request) {
        validateDates(request.drillingStartDate(), request.completionDate());
  

    Well well=new Well();
    well.setField(request.field());
    well.setWellNumber(request.wellNumber());
    well.setWellType(request.wellType());
    well.setProjectDepth(request.projectDepth());
    well.setDrillingStartDate(request.drillingStartDate());
    well.setCompletionDate(request.completionDate());

    Well saved = wellRepository.save(well);
    return WellResponseDto.from(saved);
    }

    public List<WellResponseDto> findAll() {
        return wellRepository.findAll().stream()
                .map(WellResponseDto::from)
                .toList();
    }

    public WellResponseDto findById(Long id) {
        Well well = wellRepository.findById(id)
                .orElseThrow(() -> new WellNotFoundException(id));
                return WellResponseDto.from(well);
    }
    
    public WellResponseDto update(Long id, WellRequestDto request) {
        validateDates(request.drillingStartDate(), request.completionDate());
        
        Well well = wellRepository.findById(id)
                 .orElseThrow(()->new WellNotFoundException(id));


    well.setField(request.field());
    well.setWellNumber(request.wellNumber());
    well.setWellType(request.wellType());
    well.setProjectDepth(request.projectDepth());
    well.setDrillingStartDate(request.drillingStartDate());
    well.setCompletionDate(request.completionDate());

    return WellResponseDto.from(wellRepository.save(well));

    }

    public void delete(Long id) {
        Well well = wellRepository.findById(id)
                 .orElseThrow(()->new WellNotFoundException(id));
        wellRepository.delete(well);
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start != null && end !=null && end.isBefore(start)) {
            throw new IllegalArgumentException(
                "Дата окончания не может быть раньше даты начала работ");
        }
    }


}
