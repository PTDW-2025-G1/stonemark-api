package pt.estga.shared.filters;

import org.junit.jupiter.api.Test;
import pt.estga.shared.filters.enums.FilterOperator;
import pt.estga.shared.filters.enums.LogicalOperator;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterNodeTest {

    @Test
    void validateRemovesNullChildrenOrThrowsWhenEmpty() {
        FilterNode node = new FilterNode();
        node.setOperator(LogicalOperator.AND);
        ArrayList<FilterNode> children = new ArrayList<>();
        children.add(null);
        node.setChildren(children);

        IllegalStateException ex = assertThrows(IllegalStateException.class, node::validate);
        assertTrue(ex.getMessage().contains("contains only null children"));
    }

    @Test
    void leafAndChildrenConflictThrows() {
        FilterNode node = new FilterNode();
        node.setCriteria(FilterCriteria.builder().field("id").operator(FilterOperator.EQ).value(1).build());
        node.setChildren(new ArrayList<>());

        IllegalStateException ex = assertThrows(IllegalStateException.class, node::validate);
        assertTrue(ex.getMessage().contains("Node cannot have both criteria and children"));
    }

    @Test
    void neitherCriteriaNorChildrenThrows() {
        FilterNode node = new FilterNode();
        IllegalStateException ex = assertThrows(IllegalStateException.class, node::validate);
        assertTrue(ex.getMessage().contains("Node must have either criteria or children"));
    }
}
