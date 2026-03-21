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
}
