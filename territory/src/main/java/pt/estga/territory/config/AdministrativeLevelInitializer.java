package pt.estga.territory.config;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import jakarta.annotation.Nonnull;
import pt.estga.territory.entities.AdministrativeLevel;
import pt.estga.territory.entities.Country;
import pt.estga.territory.repositories.AdministrativeLevelRepository;
import pt.estga.territory.repositories.CountryRepository;

import java.util.Optional;
import java.util.List;

/**
 * Ensures a minimal set of administrative levels exist for Portugal (PT) on startup.
 * Uses CommandLineRunner so transactional behavior is available when saving entities.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdministrativeLevelInitializer implements CommandLineRunner {

	private final AdministrativeLevelRepository administrativeLevelRepository;
	private final CountryRepository countryRepository;

	@Override
	@Transactional
	public void run(@Nonnull String... args) {
		List<String> countryCodes = List.of("PT", "ES", "FR");

		for (String code : countryCodes) {
			Optional<Country> countryOpt = countryRepository.findByCode(code);
			if (countryOpt.isEmpty()) {
				log.warn("Country with code {} not found; skipping administrative level initialization", code);
				continue;
			}

			Country country = countryOpt.get();

			createIfMissing(country, 6, "District");
			createIfMissing(country, 7, "Municipality");
			createIfMissing(country, 8, "Parish");
		}
	}

	private void createIfMissing(Country country, int osmLevel, String name) {
		if (administrativeLevelRepository.findByCountryAndOsmLevel(country, osmLevel).isEmpty()) {
			AdministrativeLevel level = AdministrativeLevel.builder()
					.country(country)
					.osmLevel(osmLevel)
					.name(name)
					.build();
			administrativeLevelRepository.save(level);
			log.info("Created administrative level '{}' (osm {}) for country {}", name, osmLevel, country.getCode());
		}
	}

}
