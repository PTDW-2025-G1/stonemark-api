package pt.estga.shared.filters;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import pt.estga.shared.filters.enums.LogicalOperator;

import java.util.Objects;

/**
 * Stateless builder that converts a {@link FilterNode} tree into a JPA {@link Specification}.
 */
@Component
public class SpecificationBuilder<T> {

    public Specification<T> build(FilterNode node) {
        if (node == null) return null;

        if (node.getCriteria() == null && (node.getChildren() == null || node.getChildren().isEmpty())) {
            return null;
        }

        node.validate();

        // Leaf node
        if (node.getCriteria() != null) {
            // Apply field mapping before creating GenericSpecification
            String mappedField = FilterFieldMapper.map(node.getCriteria().getField());
            node.getCriteria().setField(mappedField);
            return new GenericSpecification<>(node.getCriteria());
        }

        // Validate group node
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            if (node.getOperator() == LogicalOperator.AND) {
                return (root, query, cb) -> {
                    Objects.requireNonNull(root);
                    Objects.requireNonNull(query);
                    return cb.conjunction();
                };
            } else if (node.getOperator() == LogicalOperator.OR) {
                return (root, query, cb) -> {
                    Objects.requireNonNull(root);
                    Objects.requireNonNull(query);
                    return cb.disjunction();
                };
            } else {
                return null;
            }
        }

        if (node.getOperator() == null) {
            throw new IllegalArgumentException("Group node must have a logical operator");
        }

        // Check for null children
        for (FilterNode child : node.getChildren()) {
            if (child == null) {
                throw new IllegalArgumentException("Group node contains null child");
            }
        }

        Specification<T> spec = null;

        for (FilterNode child : node.getChildren()) {
            Specification<T> childSpec = build(child);
            if (childSpec == null) continue;

            if (spec == null) {
                spec = childSpec;
            } else {
                spec = node.getOperator() == LogicalOperator.OR
                        ? spec.or(childSpec)
                        : spec.and(childSpec);
            }
        }

        return spec;
    }
}