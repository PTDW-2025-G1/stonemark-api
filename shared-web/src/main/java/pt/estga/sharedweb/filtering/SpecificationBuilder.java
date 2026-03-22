package pt.estga.sharedweb.filtering;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import pt.estga.sharedweb.enums.LogicalOperator;
import pt.estga.sharedweb.models.FilterNode;

import java.util.Objects;

/**
 * Stateless builder that converts a {@link FilterNode} tree into a JPA {@link Specification}.
 */
@Component
public class SpecificationBuilder<T> {

    public Specification<T> build(FilterNode node) {
        if (node == null) return null;

        if (node.criteria() == null && node.children().isEmpty()) {
            return null;
        }

        // Leaf node
        if (node.criteria() != null) {
            return new GenericSpecification<>(node.criteria());
        }

        // Validate group node
        if (node.children().isEmpty()) {
            if (node.operator() == LogicalOperator.AND) {
                return (root, query, cb) -> {
                    Objects.requireNonNull(root);
                    Objects.requireNonNull(query);
                    return cb.conjunction();
                };
            } else if (node.operator() == LogicalOperator.OR) {
                return (root, query, cb) -> {
                    Objects.requireNonNull(root);
                    Objects.requireNonNull(query);
                    return cb.disjunction();
                };
            } else {
                return null;
            }
        }

        if (node.operator() == null) {
            throw new IllegalArgumentException("Group node must have a logical operator");
        }

        // Check for null children
        for (FilterNode child : node.children()) {
            if (child == null) {
                throw new IllegalArgumentException("Group node contains null child");
            }
        }

        Specification<T> spec = null;

        for (FilterNode child : node.children()) {
            Specification<T> childSpec = build(child);
            if (childSpec == null) continue;

            if (spec == null) {
                spec = childSpec;
            } else {
                spec = node.operator() == LogicalOperator.OR
                        ? spec.or(childSpec)
                        : spec.and(childSpec);
            }
        }

        return spec;
    }
}