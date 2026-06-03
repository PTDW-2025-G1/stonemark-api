package pt.estga.territory.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.shared.jpa.SpecBuilder;
import pt.estga.territory.dtos.AdministrativeDivisionDto;
import pt.estga.territory.dtos.DivisionFilter;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.mappers.AdministrativeDivisionMapper;
import pt.estga.territory.repositories.AdministrativeDivisionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Read-only query service for AdministrativeDivision related lookups.
 * Keeps all query logic separate from mutation/maintenance operations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DivisionService {
	
  	private final AdministrativeDivisionRepository repository;

public Page<AdministrativeDivisionDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(AdministrativeDivisionMapper::toDto);
    }

    public Page<AdministrativeDivisionDto> search(DivisionFilter filter, Pageable pageable) {
        var sb = new SpecBuilder<AdministrativeDivision>()
                .like("name", filter.name())
                .eq("parent.id", filter.parentId())
                .isNull("parent", filter.rootOnly());
        return repository.findAll(sb.build(), pageable).map(AdministrativeDivisionMapper::toDto);
    }

	public Optional<AdministrativeDivision> findById(Long id) {
		return repository.findById(id);
	}

	public List<AdministrativeDivision> findChildren(Long parentId) {
		return repository.findByParentId(parentId);
	}

	public Optional<AdministrativeDivision> findParent(Long childId) {
		return repository.findById(childId).map(AdministrativeDivision::getParent);
	}

	public List<AdministrativeDivision> findAncestors(Long childId) {
		return repository.findById(childId)
				.map(this::collectAncestors)
				.orElseGet(Collections::emptyList);
	}

	private List<AdministrativeDivision> collectAncestors(AdministrativeDivision division) {
		List<AdministrativeDivision> ancestors = new ArrayList<>();
		AdministrativeDivision current = division.getParent();
		while (current != null) {
			ancestors.add(current);
			current = current.getParent();
		}
		Collections.reverse(ancestors);
		return ancestors;
	}

	public List<AdministrativeDivision> findRoots() {
		return repository.findAllByParentIsNull();
	}

    public List<AdministrativeDivision> findByCoordinates(double latitude, double longitude) {
        return repository.findByCoordinates(latitude, longitude);
    }

    public Optional<AdministrativeDivision> findLowestContainingDivision(double latitude, double longitude) {
        return repository.findLowestContainingDivision(latitude, longitude);
    }
}


