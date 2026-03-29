package pt.estga.territory.repositories;

import pt.estga.shared.repositories.BaseRepository;
import pt.estga.territory.entities.AdministrativeLevel;
import pt.estga.territory.entities.Country;

import java.util.Optional;

public interface AdministrativeLevelRepository extends BaseRepository<AdministrativeLevel, Integer> {

	Optional<AdministrativeLevel> findByCountryAndOsmLevel(Country country, int osmLevel);

}
