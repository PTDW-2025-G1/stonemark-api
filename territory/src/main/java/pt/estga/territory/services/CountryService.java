package pt.estga.territory.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.territory.entities.Country;
import pt.estga.territory.repositories.CountryRepository;

import java.util.List;
import java.util.Optional;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;

    public List<Country> findAll() {
        return countryRepository.findAll();
    }

    public Optional<Country> findById(Integer id) {
        return countryRepository.findById(id);
    }

    /**
     * Normalize the provided country code and return the matching Country if present.
     * Normalization trims whitespace and upper-cases the code using the ROOT locale.
     * Returns null when no matching country is found.
     */
    public Country findByCode(String code) {
        if (code == null) return null;
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) return null;
        return countryRepository.findByCode(normalized).orElse(null);
    }

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
