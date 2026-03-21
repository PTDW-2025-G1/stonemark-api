package pt.estga.shared.filters;

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
        if (input == null) {
            return null;
        }

        // Validate and normalize leaf nodes
        if (input.isLeaf()) {
            if (input.getCriteria() == null || input.getCriteria().getField() == null) {
                throw new IllegalArgumentException("Leaf node must have valid criteria with a field");
            }

            // Map the field using FilterFieldMapper
            String mappedField = FilterFieldMapper.map(input.getCriteria().getField());
            input.getCriteria().setField(mappedField);
        }

        // Validate and normalize group nodes
        if (input.isGroup()) {
            if (input.getChildren() == null || input.getChildren().isEmpty()) {
                throw new IllegalArgumentException("Group node must have non-empty children");
            }

            // Recursively normalize children
            List<FilterNode> normalizedChildren = input.getChildren().stream()
                    .filter(Objects::nonNull)
                    .map(FilterNormalizer::normalize)
                    .toList();

            input.setChildren(normalizedChildren);
        }

        return input;
    }

    /**
     * Validates and normalizes the given FilterCriteria.
     *
     * @param criteria The FilterCriteria to validate and normalize.
     * @return A normalized FilterCriteria object.
     * @throws IllegalArgumentException if the criteria is invalid.
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
