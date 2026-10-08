package com.example.drillix.Section;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.drillix.Well.Well;
import com.example.drillix.Well.WellNotFoundException;
import com.example.drillix.Well.WellRepository;



@Service 
public class SectionService {

    private final SectionRepository sectionRepository;
    private final WellRepository wellRepository;
    public SectionService(SectionRepository sectionRepository,
                          WellRepository wellRepository) {
        this.sectionRepository=sectionRepository;   
        this.wellRepository=wellRepository;

    }
   
    //создание секции внутри скважины
    public SectionResponseDto create(Long wellId, SectionRequestDto request) {
        Well well = wellRepository.findById(wellId)
                    .orElseThrow(()->new WellNotFoundException(wellId));
        validateBounds(request);
        validateOverlap(well, request, null);

        Section section = new Section();
        section.setWell(well);
        section.setType(request.type());
        section.setDepthFrom(request.depthFrom());
        section.setDepthTo(request.depthTo());
        section.setBitDiametr(request.bitDiametr());
        section.setTrajectoryTask(request.trajectoryTask());
        section.setMaxZenithAngle(request.maxZenithAngele());
        section.setDrillingCondition(request.drillingConditions());

        return SectionResponseDto.from(sectionRepository.save(section));
    
        }

        //секции скважины по порядку 
        public List<SectionResponseDto> findAllByWell(Long wellId) {
            Well well = getWellOrThrow(wellId);
            return sectionRepository.findAllByWellOrderByDepthFromAsc(well).stream()
                    .map(SectionResponseDto::from)
                    .toList();
        }

        //достать одну секцию
        public SectionResponseDto findById(Long wellId, Long sectionId) {
            Section section = getSectionOfWell(wellId, sectionId);
            return SectionResponseDto.from(section);
        }

        //корректировка секции
        public SectionResponseDto update(Long wellId, Long sectionId, SectionRequestDto request) {
            Section section=getSectionOfWell(wellId, sectionId);
            validateBounds(request);
            validateOverlap(section.getWell(), request, section.getId());

            section.setType(request.type());
            section.setDepthFrom(request.depthFrom());
            section.setDepthTo(request.depthTo());
            section.setBitDiametr(request.bitDiametr());
            section.setTrajectoryTask(request.trajectoryTask());
            section.setMaxZenithAngle(request.maxZenithAngele());
            section.setDrillingCondition(request.drillingConditions());

            return SectionResponseDto.from(sectionRepository.save(section));
        }

        //удаление секции
        public void delete(Long wellId, Long sectionId) {
            sectionRepository.delete(getSectionOfWell(wellId, sectionId));
        }

        // === Приватные помощники — вся валидация FR-2.3 здесь ===

        private Well getWellOrThrow(Long wellId) {
            return wellRepository.findById(wellId)
                    .orElseThrow(()->new WellNotFoundException(wellId));
        }

        //достать секцию и убедится что она принадлежит определенной скважине
        private Section getSectionOfWell(Long wellId, Long sectionId) {
            Well well = getWellOrThrow(wellId);
            return sectionRepository.findById(sectionId)
                        .filter(s->s.getWell().getId().equals(well.getId()))
                        .orElseThrow(()-> new SectionNotFoundException(sectionId));
        }

        //забой ниже устья
        private void validateBounds(SectionRequestDto request) {
            if(request.depthTo()<=request.depthFrom()) {
                throw new IllegalArgumentException(
                    "Забой интервала (%d) должен быть больше кровли (%d)"
                    .formatted(request.depthTo(), request.depthFrom()));
            }
        }

        //интервалы одной секции не могут пересекаться
        private void validateOverlap(Well well, SectionRequestDto request, Long excludeId) {
            List<Section> overlapping=sectionRepository
                    .findOverlapping(well, request.depthFrom(), request.depthTo());

                    boolean hasConflict=overlapping.stream()
                            .anyMatch(s->excludeId==null||!s.getId().equals(excludeId));
                    if (hasConflict) {
                        throw new IllegalArgumentException(
                            "Интервал [%d–%d] пересекается с существующей секцией"
                                    .formatted(request.depthFrom(), request.depthTo()));
                    }        
        }
    
    
    
}
