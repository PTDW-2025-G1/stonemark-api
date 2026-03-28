package pt.estga.territory.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.territory.entities.Country;
import pt.estga.territory.repositories.CountryRepository;

/**
 * Ensures a minimal set of countries exist in the database on application startup.
 * Uses CommandLineRunner so transactional behavior is available when saving entities.
 */
@Component
@RequiredArgsConstructor
public class CountryInitializer implements CommandLineRunner {

    private final CountryRepository countryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        createIfMissing("PT", "Portugal");
        createIfMissing("ES", "Spain");
        createIfMissing("FR", "France");
    }

    private void createIfMissing(String code, String name) {
        if (countryRepository.findByCode(code).isEmpty()) {
            Country country = Country.builder()
                    .code(code)
                    .name(name)
                    .build();
            countryRepository.save(country);
        }
    }

}
