package pt.estga.shared.jpa;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class SpecBuilder<T> {

    private final List<Specification<T>> specifications = new ArrayList<>();

    public SpecBuilder<T> eq(String field, Object value) {
        if (value != null) {
            specifications.add((root, query, cb) -> cb.equal(root.get(field), value));
        }
        return this;
    }

    public SpecBuilder<T> isNull(String field, Boolean value) {
        if (value != null && value) {
            specifications.add((root, query, cb) -> cb.isNull(root.get(field)));
        }
        return this;
    }

    public SpecBuilder<T> like(String field, String value) {
        if (value != null && !value.isBlank()) {
            specifications.add((root, query, cb)
                    -> cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
        }
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public SpecBuilder<T> afterOrEqual(String field, Comparable<?> value) {
        if (value != null) {
            specifications.add((root, query, cb)
                    -> cb.greaterThanOrEqualTo(root.get(field), (Comparable) value));
        }
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public SpecBuilder<T> beforeOrEqual(String field, Comparable<?> value) {
        if (value != null) {
            specifications.add((root, query, cb)
                    -> cb.lessThanOrEqualTo(root.get(field), (Comparable) value));
        }
        return this;
    }

    public SpecBuilder<T> isTrue(String field, Boolean value) {
        if (value != null) {
            specifications.add((root, query, cb) -> value ? cb.isTrue(root.get(field)) : cb.isFalse(root.get(field)));
        }
        return this;
    }

    public Specification<T> build() {
        if (specifications.isEmpty()) {
            return Specification.where((Specification<T>) null);
        }
        Specification<T> result = specifications.getFirst();
        for (int i = 1; i < specifications.size(); i++) {
            result = result.and(specifications.get(i));
        }
        return result;
    }
}
