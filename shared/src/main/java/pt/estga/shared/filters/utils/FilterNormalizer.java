package pt.estga.shared.filters.utils;

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
        return normalize(input, Integer.MAX_VALUE);
    }

    /**
     * Normalizes and validates the given FilterNode with a depth limit.
     *
     * @param input the input FilterNode to normalize
     * @param maxDepth the maximum depth allowed for the tree
     * @return the normalized FilterNode
     */
    public static FilterNode normalize(FilterNode input, int maxDepth) {
        if (input == null) {
            return null;
        }

        if (maxDepth <= 0) {
            throw new IllegalArgumentException("Tree depth exceeds the maximum allowed depth");
        }

        // Perform structural validation
        input.validate();

        // Normalize leaf nodes
        if (input.isLeaf()) {
            if (input.criteria() == null || input.criteria().getField() == null) {
                throw new IllegalArgumentException("Leaf node must have valid criteria with a field");
            }

            FilterCriteria normalizedCriteria = normalize(input.criteria());

            return FilterNode.builder()
                    .operator(input.operator())
                    .criteria(normalizedCriteria)
                    .build();
        }

        // Normalize group nodes
        if (input.isGroup()) {
            if (input.operator() == null) {
                throw new IllegalArgumentException("Group node must have a valid operator");
            }

            if (input.children().stream().anyMatch(java.util.Objects::isNull)) {
                throw new IllegalArgumentException("Null child in filter tree");
            }

            // Recursively normalize children
            List<FilterNode> normalizedChildren = input.children().stream()
                    .map(child -> normalize(child, maxDepth - 1))
                    .toList();

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
            }
            case LIKE, EQ, NE, GT, LT, GTE, LTE -> {
                if (criteria.getValue() == null) {
                    throw new IllegalArgumentException("Operator " + criteria.getOperator() + " requires a non-null value");
                }
            }
            default -> {
                // No additional validation needed for other operators.
            }
        }

        return criteria;
    }
}
