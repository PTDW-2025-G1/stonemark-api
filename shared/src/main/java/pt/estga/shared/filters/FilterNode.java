package pt.estga.shared.filters;

import lombok.*;
import pt.estga.shared.filters.enums.LogicalOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Node in a filter expression tree. A node is either a leaf (contains {@link FilterCriteria})
 * or a group (contains child nodes and a {@link LogicalOperator}).
 * <p>
 * Expected JSON shape when received from clients:
 * {
 *   "operator": "AND|OR",           // required for group nodes
 *   "children": [ { ... }, { ... } ], // group nodes
 *   "criteria": { ... }               // leaf nodes
 * }
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FilterNode {
    private LogicalOperator operator;
    private List<FilterNode> children;
    private FilterCriteria criteria;

    public boolean isLeaf() {
        return criteria != null;
    }

    public boolean isGroup() {
        return children != null && !children.isEmpty();
    }

    public List<FilterNode> getChildren() {
        return children == null ? List.of() : new ArrayList<>(children);
    }

    public void validate() {
        Integer originalChildrenCount = null;
        if (children != null) {
            originalChildrenCount = children.size();
            this.children = new ArrayList<>(children);
            this.children.removeIf(Objects::isNull);
        }

        if (criteria != null && children != null) {
            throw new IllegalStateException("Node cannot have both criteria and children");
        }

        if (criteria == null) {
            if (children == null || children.isEmpty()) {
                if (originalChildrenCount != null && originalChildrenCount > 0) {
                    throw new IllegalStateException(
                            "Group node contains only null children; operator=" + operator + ", originalCount=" + originalChildrenCount
                    );
                }
                throw new IllegalStateException("Node must have either criteria or children");
            }
        }

        if (isGroup() && operator == null) {
            throw new IllegalStateException("Group node must have a logical operator");
        }

        // Additional validation for leaf nodes
        if (isLeaf() && criteria.getOperator() == null) {
            throw new IllegalStateException("Leaf node must have a valid operator in its criteria");
        }
    }

    @Override
    public String toString() {
        return "FilterNode{" +
                "operator=" + operator +
                ", childrenCount=" + (children == null ? 0 : children.size()) +
                ", criteria=" + criteria +
                '}';
    }
}
