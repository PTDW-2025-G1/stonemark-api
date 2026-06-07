package pt.estga.territory.repositories;

import pt.estga.commoninfra.repositories.BaseRepository;
import pt.estga.territory.entities.Country;
import java.util.Optional;

public interface CountryRepository extends BaseRepository<Country, Integer> {

	Optional<Country> findByCode(String code);
}
