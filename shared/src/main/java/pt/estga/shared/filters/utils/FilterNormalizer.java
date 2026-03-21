package pt.estga.shared.filters.utils;

import jakarta.persistence.criteria.JoinType;
import pt.estga.shared.filters.models.FilterCriteria;
import pt.estga.shared.filters.models.FilterNode;

import java.util.List;
import java.util.Objects;

/**
 * Utility class for normalizing and validating FilterNode objects.
 */
public class FilterNormalizer {

    /**
     * Normalizes and validates the given FilterNode.
     *
     * @param input the input FilterNode to normalize
     * @return the normalized FilterNode
     */
    public static FilterNode normalize(FilterNode input) {
        return normalize(input, Integer.MAX_VALUE, JoinType.LEFT);
    }

    /**
     * Normalizes and validates the given FilterNode with a depth limit.
     *
     * @param input the input FilterNode to normalize
     * @param maxDepth the maximum depth allowed for the tree
     * @return the normalized FilterNode
     */
    public static FilterNode normalize(FilterNode input, int maxDepth) {
        return normalize(input, maxDepth, JoinType.LEFT);
    }

    /**
     * Normalizes and validates the given FilterNode with a depth limit and join type.
     * The join type is passed through to created FilterCriteria instances so callers
     * can choose LEFT or INNER joins when resolving nested paths.
     *
     * @param input the input FilterNode to normalize
     * @param maxDepth the maximum depth allowed for the tree
     * @param joinType the JPA join type to use when building criteria
     * @return the normalized FilterNode
     */
    public static FilterNode normalize(FilterNode input, int maxDepth, JoinType joinType) {
        if (input == null) {
            return null;
        }

        if (maxDepth <= 0) {
            throw new IllegalArgumentException("Tree depth exceeds the maximum allowed depth. Current depth: " + maxDepth);
        }

        // Perform structural validation
        input.validate();

        // Normalize leaf nodes
        if (input.isLeaf()) {
            if (input.criteria() == null || input.criteria().getField() == null) {
                throw new IllegalArgumentException("Leaf node must have valid criteria with a field");
            }
            FilterCriteria normalizedCriteria = normalize(input.criteria(), joinType);

            return FilterNode.builder()
                    .criteria(normalizedCriteria)
                    .build();
        }

        // Normalize group nodes
        if (input.isGroup()) {
            if (input.operator() == null) {
                throw new IllegalArgumentException("Group node must have a valid operator");
            }

            // Explicitly reject group nodes that also carry criteria to make intent
            // clearer and to guard against future ambiguous shapes where both
            // children and criteria are present.
            if (input.criteria() != null) {
                throw new IllegalArgumentException("Group node must not carry criteria");
            }

            // Recursively normalize children
            List<FilterNode> normalizedChildren = input.children().stream()
                    .map(child -> normalize(child, maxDepth - 1, joinType))
                    .map(Objects::requireNonNull)
                    .toList();

            // Defensively ensure group nodes have at least one child after normalization.
            // This is guarded by FilterNode.validate() above, but double-check here to
            // protect against unexpected transformations during recursive normalization.
            if (normalizedChildren.isEmpty()) {
                throw new IllegalArgumentException("Group node must have non-empty children after normalization");
            }

            return FilterNode.builder()
                    .operator(input.operator())
                    .children(normalizedChildren)
                    .build();
        }

        throw new IllegalArgumentException("Invalid FilterNode: neither leaf nor group");
    }

    /**
     * Normalizes and validates the given FilterCriteria.
     *
     * @param criteria the FilterCriteria to normalize
     * @return the normalized FilterCriteria
     */
    public static FilterCriteria normalize(FilterCriteria criteria) {
        return normalize(criteria, JoinType.LEFT);
    }

    /**
     * Normalizes and validates the given FilterCriteria using the provided join type.
     * Additional safety checks are performed for LIKE operator and list-based operators.
     *
     * @param criteria the FilterCriteria to normalize
     * @param joinType the join type to use when creating the validated builder
     * @return the normalized FilterCriteria
     */
    public static FilterCriteria normalize(FilterCriteria criteria, JoinType joinType) {
        Objects.requireNonNull(criteria, "FilterCriteria cannot be null");

        if (criteria.getField() == null || criteria.getField().isBlank()) {
            throw new IllegalArgumentException("Filter field cannot be null or blank");
        }

        if (criteria.getOperator() == null) {
            throw new IllegalArgumentException("Filter operator cannot be null");
        }

        switch (criteria.getOperator()) {
            case IN, BETWEEN -> {
                if (!(criteria.getValue() instanceof List<?> values) || values.isEmpty()) {
                    throw new IllegalArgumentException("Operator " + criteria.getOperator() + " requires a non-empty list of values");
                }

                // Trim string elements within lists for defensive normalization
                List<?> normalizedList = values.stream()
                        .map(v -> v instanceof String s ? s.trim() : v)
                        .toList();

                return FilterCriteria.validatedBuilder(
                        criteria.getField().trim(),
                        criteria.getOperator(),
                        normalizedList,
                        criteria.getLikeMode(),
                        criteria.isCaseSensitive(),
                        joinType
                );
            }
            case LIKE -> {
                if (criteria.getValue() == null) {
                    throw new IllegalArgumentException("Operator LIKE requires a non-null value");
                }
                if (!(criteria.getValue() instanceof String)) {
                    throw new IllegalArgumentException("LIKE operator requires a String value");
                }
                return FilterCriteria.validatedBuilder(
                        criteria.getField().trim(),
                        criteria.getOperator(),
                        ((String) criteria.getValue()).trim(),
                        criteria.getLikeMode(),
                        criteria.isCaseSensitive(),
                        joinType
                );
            }
            case EQ, NE, GT, LT, GTE, LTE -> {
                if (criteria.getValue() == null) {
                    throw new IllegalArgumentException("Operator " + criteria.getOperator() + " requires a non-null value");
                }
                Object val = criteria.getValue() instanceof String s ? s.trim() : criteria.getValue();
                return FilterCriteria.validatedBuilder(
                        criteria.getField().trim(),
                        criteria.getOperator(),
                        val,
                        criteria.getLikeMode(),
                        criteria.isCaseSensitive(),
                        joinType
                );
            }
            default -> throw new IllegalArgumentException("Unsupported operator: " + criteria.getOperator());
        }
    }
}
