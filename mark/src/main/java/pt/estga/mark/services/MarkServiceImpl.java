package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.mark.mappers.MarkMapper;
import pt.estga.mark.mappers.MarkOccurrenceMapper;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.markapi.MarkService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarkServiceImpl implements MarkService {

    private final MarkRepository markRepository;
    private final MarkOccurrenceRepository occurrenceRepository;

    @Override
    public Optional<MarkDto> findMarkById(Long id) {
        return markRepository.findById(id).map(MarkMapper::toDto);
    }

    @Override
    public Optional<MarkOccurrenceDto> findOccurrenceById(Long id) {
        return occurrenceRepository.findById(id).map(MarkOccurrenceMapper::toDto);
    }
}
