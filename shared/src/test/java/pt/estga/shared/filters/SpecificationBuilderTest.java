package pt.estga.shared.filters;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.jpa.domain.Specification;
import pt.estga.shared.filters.enums.FilterOperator;
import pt.estga.shared.filters.enums.LogicalOperator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SpecificationBuilderTest {

    static class Person {
        String name;
        Integer age;
        Boolean active;
        enum Role { ADMIN, USER }
        Role role;
    }

    // Minimal helper to mock JPA Criteria API objects
    private <T> Root<T> mockRoot() {
        return mock(Root.class);
    }

    private <T> CriteriaQuery<T> mockQuery() {
        return mock(CriteriaQuery.class);
    }

    private CriteriaBuilder mockBuilder() {
        return mock(CriteriaBuilder.class);
    }

    @Test
    void build_nullNode_returnsNull() {
        SpecificationBuilder<Person> builder = new SpecificationBuilder<>();
        assertNull(builder.build(null));
    }

    @Test
    void build_leafNode_returnsSpecification() {
        FilterCriteria criteria = FilterCriteria.builder()
                .field("name")
                .operator(FilterOperator.EQ)
                .value("Alice")
                .build();
        FilterNode node = FilterNode.builder().criteria(criteria).build();

        SpecificationBuilder<Person> builder = new SpecificationBuilder<>();
        Specification<Person> spec = builder.build(node);

        assertNotNull(spec);
    }

    @Test
    void build_groupNode_andOperator_returnsSpecification() {
        FilterCriteria c1 = FilterCriteria.builder().field("age").operator(FilterOperator.GT).value(18).build();
        FilterCriteria c2 = FilterCriteria.builder().field("active").operator(FilterOperator.EQ).value(true).build();
        FilterNode n1 = FilterNode.builder().criteria(c1).build();
        FilterNode n2 = FilterNode.builder().criteria(c2).build();

        FilterNode group = FilterNode.builder()
                .operator(LogicalOperator.AND)
                .children(List.of(n1, n2))
                .build();

        SpecificationBuilder<Person> builder = new SpecificationBuilder<>();
        Specification<Person> spec = builder.build(group);

        assertNotNull(spec);
    }

    @Test
    void build_groupNode_orOperator_returnsSpecification() {
        FilterCriteria c1 = FilterCriteria.builder().field("age").operator(FilterOperator.LT).value(65).build();
        FilterCriteria c2 = FilterCriteria.builder().field("active").operator(FilterOperator.EQ).value(false).build();
        FilterNode n1 = FilterNode.builder().criteria(c1).build();
        FilterNode n2 = FilterNode.builder().criteria(c2).build();

        FilterNode group = FilterNode.builder()
                .operator(LogicalOperator.OR)
                .children(List.of(n1, n2))
                .build();

        SpecificationBuilder<Person> builder = new SpecificationBuilder<>();
        Specification<Person> spec = builder.build(group);

        assertNotNull(spec);
    }

    @Test
    void build_nodeWithBothCriteriaAndChildren_throws() {
        FilterCriteria c1 = FilterCriteria.builder().field("age").operator(FilterOperator.GT).value(18).build();
        FilterNode node = FilterNode.builder()
                .criteria(c1)
                .children(List.of(FilterNode.builder().criteria(c1).build()))
                .build();

        SpecificationBuilder<Person> builder = new SpecificationBuilder<>();
        assertThrows(IllegalStateException.class, node::validate);
    }

    @Test
    void build_nodeWithNeitherCriteriaNorChildren_throws() {
        FilterNode node = FilterNode.builder().build();
        assertThrows(IllegalStateException.class, node::validate);
    }

    @Test
    void build_leafNode_likeOperator_returnsSpecification() {
        FilterCriteria criteria = FilterCriteria.builder()
                .field("name")
                .operator(FilterOperator.LIKE)
                .value("A%")
                .build();
        FilterNode node = FilterNode.builder().criteria(criteria).build();

        SpecificationBuilder<Person> builder = new SpecificationBuilder<>();
        Specification<Person> spec = builder.build(node);

        assertNotNull(spec);
    }

    @Nested
    class ParameterizedOperatorTests {
        static Object[][] operatorValueProvider() {
            return new Object[][] {
                    {FilterOperator.EQ, "name", "Alice"},
                    {FilterOperator.LIKE, "name", "A%"},
                    {FilterOperator.GT, "age", 18},
                    {FilterOperator.LT, "age", 65},
                    {FilterOperator.IS_NULL, "name", null},
                    {FilterOperator.IS_NOT_NULL, "name", null},
            };
        }

        @ParameterizedTest
        @MethodSource("operatorValueProvider")
        void build_leafNode_variousOperators_returnsSpecification(FilterOperator op, String field, Object value) {
            FilterCriteria.FilterCriteriaBuilder builder = FilterCriteria.builder().field(field).operator(op);
            if (op != FilterOperator.IS_NULL && op != FilterOperator.IS_NOT_NULL) builder.value(value);
            FilterCriteria criteria = builder.build();
            FilterNode node = FilterNode.builder().criteria(criteria).build();

            SpecificationBuilder<Person> builderSpec = new SpecificationBuilder<>();
            Specification<Person> spec = builderSpec.build(node);

            assertNotNull(spec);
        }
    }
}