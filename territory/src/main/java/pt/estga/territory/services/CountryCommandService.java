package pt.estga.territory.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.territory.entities.Country;
import pt.estga.territory.repositories.CountryRepository;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;
import pt.estga.territory.dtos.CountryRequestDto;
import pt.estga.territory.mappers.CountryMapper;

@Service
@RequiredArgsConstructor
public class CountryCommandService {

    private final CountryRepository countryRepository;
    private final CountryMapper mapper;

    @Transactional
    public Country create(Country country) {
        return countryRepository.save(country);
    }

    public Country create(CountryRequestDto dto) {
        Country entity = mapper.toEntity(dto);
        return countryRepository.save(entity);
    }

    @Transactional
    public Country update(Country country) {
        if (country.getId() == null) {
            throw new ResourceNotFoundException("Country id must not be null for update");
        }

        Country existing = countryRepository.findById(country.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Country with id " + country.getId() + " not found"));

        mapper.update(country, existing);

        return countryRepository.save(existing);
    }

    public Country update(Integer id, CountryRequestDto dto) {
        Country existing = countryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Country with id " + id + " not found"));

        mapper.updateFromRequest(dto, existing);

        return countryRepository.save(existing);
    }

    @Transactional
    public void deleteById(Integer id) {
        Country existing = countryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Country with id " + id + " not found"));

        countryRepository.softDelete(existing);
    }
}
