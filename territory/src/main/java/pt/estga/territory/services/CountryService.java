package pt.estga.territory.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.territory.entities.Country;
import pt.estga.territory.repositories.CountryRepository;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;

    @Transactional
    public Country create(Country country) {
        return countryRepository.save(country);
    }

    @Transactional
    public Country update(Country country) {
        return countryRepository.save(country);
    }

    @Transactional
    public void deleteById(Integer id) {
        countryRepository.findById(id).ifPresent(c -> countryRepository.deleteById(id));
    }
}
