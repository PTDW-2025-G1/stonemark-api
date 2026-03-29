package pt.estga.territory.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.territory.dtos.AdministrativeDivisionDto;
import pt.estga.territory.dtos.AdministrativeDivisionRequestDto;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.mappers.AdministrativeDivisionMapper;
import pt.estga.territory.repositories.AdministrativeDivisionRepository;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class AdministrativeDivisionCommandService {

    private final AdministrativeDivisionRepository repository;
    private final AdministrativeDivisionMapper mapper;

    @Transactional
    public AdministrativeDivision create(AdministrativeDivision division) {
        return repository.save(division);
    }

    @Transactional
    public AdministrativeDivision update(AdministrativeDivision division) {
        if (division.getId() == null) {
            throw new ResourceNotFoundException("Division id must not be null for update");
        }

        AdministrativeDivision existing = repository.findById(division.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AdministrativeDivision with id " + division.getId() + " not found"));

        mapper.update(division, existing);

        return repository.save(existing);
    }

    public AdministrativeDivision updateFromDto(Long id, AdministrativeDivisionRequestDto divisionDto) {
        AdministrativeDivision existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AdministrativeDivision with id " + id + " not found"));

        mapper.updateFromRequest(divisionDto, existing);

        return repository.save(existing);
    }

    public void deleteById(Long id) {
        AdministrativeDivision existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AdministrativeDivision with id " + id + " not found"));

        repository.softDelete(existing);
    }
}
