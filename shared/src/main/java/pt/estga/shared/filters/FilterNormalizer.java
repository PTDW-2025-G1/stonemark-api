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
}
