package pt.estga.shared.filters;

import jakarta.persistence.criteria.Path;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GenericSpecificationTest {

    enum TestEnum { A, B }

    private <T> GenericSpecification<T> createSpec(FilterCriteria criteria) throws Exception {
        Constructor<GenericSpecification> ctor = GenericSpecification.class.getDeclaredConstructor(FilterCriteria.class);
        ctor.setAccessible(true);
        return (GenericSpecification<T>) ctor.newInstance(criteria);
    }

    @SuppressWarnings("unchecked")
    private static <T> Path<T> createDummyPath(Class<T> type) {
        return (Path<T>) java.lang.reflect.Proxy.newProxyInstance(
                Path.class.getClassLoader(),
                new Class[]{Path.class},
                (proxy, method, args) -> {
                    if ("getJavaType".equals(method.getName())) return type;
                    throw new UnsupportedOperationException("Not implemented: " + method.getName());
                }
        );
    }

    @Test
    void convertValueStringToInteger() throws Exception {
        GenericSpecification<?> spec = createSpec(FilterCriteria.builder().field("id").build());

        Method convert = GenericSpecification.class.getDeclaredMethod("convertSingle", Path.class, Object.class);
        convert.setAccessible(true);

        Path<Integer> path = createDummyPath(Integer.class);

        Object single = convert.invoke(spec, path, "123");
        assertEquals(123, single);
    }

    @Test
    void convertValueListOfStringsToIntegers() throws Exception {
        GenericSpecification<?> spec = createSpec(FilterCriteria.builder().field("id").build());

       Method convertList = GenericSpecification.class.getDeclaredMethod("convertList", Path.class, List.class);
        convertList.setAccessible(true);

        Path<Integer> path = createDummyPath(Integer.class);

        Object converted = convertList.invoke(spec, path, List.of("1", "2", "3"));
        assertInstanceOf(List.class, converted);
        assertEquals(List.of(1, 2, 3), converted);
    }

    @Test
    void invalidEnumValueThrows() throws Exception {
        GenericSpecification<?> spec = createSpec(FilterCriteria.builder().field("status").build());

        Method convert = GenericSpecification.class.getDeclaredMethod("convertSingle", Path.class, Object.class);
        convert.setAccessible(true);

        Path<TestEnum> path = createDummyPath(TestEnum.class);

        Exception ex = assertThrows(Exception.class, () -> convert.invoke(spec, path, "Z"));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("Invalid enum value"));
    }

    @Test
    void betweenStartGreaterThanEndThrows() throws Exception {
        GenericSpecification<?> spec = createSpec(FilterCriteria.builder().field("amount").build());

        Method between = GenericSpecification.class.getDeclaredMethod(
                "betweenPredicate",
                jakarta.persistence.criteria.CriteriaBuilder.class,
                Path.class,
                Object.class
        );
        between.setAccessible(true);

        Path<Integer> path = createDummyPath(Integer.class);

        Exception ex = assertThrows(Exception.class, () -> between.invoke(spec, null, path, List.of("10", "1")));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().contains("BETWEEN start value must be less than or equal to end value"));
    }

    @Test
    void inWithNullElementThrows() throws Exception {
        GenericSpecification<?> spec = createSpec(FilterCriteria.builder().field("id").build());

        Method inMethod = GenericSpecification.class.getDeclaredMethod(
                "inPredicate",
                jakarta.persistence.criteria.CriteriaBuilder.class,
                Path.class,
                Object.class
        );
        inMethod.setAccessible(true);

        Path<Integer> path = createDummyPath(Integer.class);

        Exception ex = assertThrows(Exception.class, () -> inMethod.invoke(spec, null, path, List.of(1, null, 3)));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().contains("IN list contains null element"));
    }

    @Test
    void escapeLikeEscapesPercentAndUnderscore() throws Exception {
        GenericSpecification<?> spec = createSpec(FilterCriteria.builder().field("name").build());

        Method escape = GenericSpecification.class.getDeclaredMethod("escapeLike", String.class, char.class);
        escape.setAccessible(true);

        String escaped = (String) escape.invoke(spec, "%_test\\", '\\');
        assertEquals("\\%\\_test\\\\", escaped);
    }
}