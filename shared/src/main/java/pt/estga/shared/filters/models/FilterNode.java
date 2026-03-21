package pt.estga.shared.filters.models;

import lombok.*;
import org.jspecify.annotations.NonNull;
import pt.estga.shared.filters.enums.LogicalOperator;

import java.util.ArrayList;
import java.util.List;

/**
 * Node in a filter expression tree. A node is either a leaf (contains {@link FilterCriteria})
 * or a group (contains child nodes and a {@link LogicalOperator}).
 * <p>
 * Expected JSON shape when received from clients:
 * {
 * "operator": "AND|OR",           // required for group nodes
 * "children": [ { ... }, { ... } ], // group nodes
 * "criteria": { ... }               // leaf nodes
 * }
 */
@Builder
public record FilterNode(LogicalOperator operator, List<FilterNode> children, FilterCriteria criteria) {
    public boolean isLeaf() {
        return criteria != null;
    }

    public boolean isGroup() {
        return children != null && !children.isEmpty();
    }

    @Override
    public List<FilterNode> children() {
        return children == null ? List.of() : new ArrayList<>(children);
    }

    @Override
    public @NonNull String toString() {
        return "FilterNode{" +
                "operator=" + operator +
                ", childrenCount=" + (children == null ? 0 : children.size()) +
                ", criteria=" + criteria +
                '}';
    }

    /**
     * Validates the structure of this FilterNode.
     * Ensures that a node is either a leaf or a group, but not both.
     * Also validates additional constraints for group and leaf nodes.
     *
     * @throws IllegalStateException if the node structure is invalid.
     */
    public void validate() {
        boolean isLeaf = isLeaf();
        boolean isGroup = isGroup();

        if (isLeaf && isGroup) {
            throw new IllegalStateException("A FilterNode cannot be both a leaf and a group.");
        }

        if (!isLeaf && !isGroup) {
            throw new IllegalStateException("A FilterNode must be either a leaf or a group.");
        }

        if (isGroup && operator == null) {
            throw new IllegalStateException("Group node must have an operator.");
        }

        if (isLeaf && operator != null) {
            throw new IllegalStateException("Leaf node cannot have an operator.");
        }

        if (isGroup && children.stream().allMatch(java.util.Objects::isNull)) {
            throw new IllegalStateException("Group node has only null children.");
        }

        // Recursively validate child nodes if this is a group
        if (isGroup) {
            for (FilterNode child : children) {
                if (child != null) {
                    child.validate();
                }
            }
        }
    }
}
