package pt.estga.territory.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.repositories.AdministrativeDivisionRepository;

import java.util.List;
import java.util.Optional;

/**
 * Read-only query service for AdministrativeDivision related lookups.
 * Keeps all query logic separate from mutation/maintenance operations.
 */
@Service
@RequiredArgsConstructor
public class AdministrativeDivisionQueryService {

	private final AdministrativeDivisionRepository repository;

	public Page<AdministrativeDivision> search(Specification<AdministrativeDivision> specification, Pageable pageable) {
		return repository.findAll(specification, pageable);
	}

	public Optional<AdministrativeDivision> findById(Long id) {
		return repository.findById(id);
	}

	public List<AdministrativeDivision> findByOsmAdminLevel(int adminLevel) {
		return repository.findByOsmAdminLevel(adminLevel);
	}

	public List<AdministrativeDivision> findChildren(Long parentId) {
		return repository.findByParentId(parentId);
	}

	public Optional<AdministrativeDivision> findParentByGeometry(Long childId, int parentLevel) {
		return repository.findParentByGeometry(childId, parentLevel);
	}

	public Optional<AdministrativeDivision> findParent(Long childId) {
		return repository.findById(childId).map(AdministrativeDivision::getParent);
	}

	public List<AdministrativeDivision> findByCoordinates(double latitude, double longitude) {
		return repository.findByCoordinates(latitude, longitude);
	}

	public List<AdministrativeDivision> findWithMonuments(int adminLevel) {
		return repository.findWithMonuments(adminLevel);
	}

	public List<AdministrativeDivision> findAllRoots() {
		return repository.findAllByParentIsNull();
	}
}


