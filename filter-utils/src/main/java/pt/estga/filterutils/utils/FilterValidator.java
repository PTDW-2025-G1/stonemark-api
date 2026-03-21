package pt.estga.filterutils.utils;

import pt.estga.filterutils.models.FilterNode;
import pt.estga.filterutils.models.SortCriteria;
import pt.estga.filterutils.annotations.Filterable;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility to validate that FilterNode and SortCriteria reference only allowed fields
 * as indicated by the {@link Filterable} annotation on DTO fields.
 */
public final class FilterValidator {

    private static final Map<Class<?>, Set<String>> CACHE = new ConcurrentHashMap<>();

    private FilterValidator() {
    }

    public static void validate(FilterNode node, Class<?> dtoClass) {
        if (node == null) return;
        Set<String> allowed = getFilterableFields(dtoClass);

        if (node.isLeaf()) {
            var criteria = node.criteria();
            if (criteria != null) {
                String field = criteria.getField();
                if (field != null && !field.isBlank()) {
                    String root = field.contains(".") ? field.split("\\.")[0] : field;
                    if (!allowed.contains(root)) {
                        throw new IllegalArgumentException("Filtering by '" + field + "' is not allowed for " + dtoClass.getSimpleName());
                    }
                }
            }
        }

        for (FilterNode child : node.children()) {
            validate(child, dtoClass);
        }
    }

    public static void validateSort(java.util.List<SortCriteria> sort, Class<?> dtoClass) {
        if (sort == null || sort.isEmpty()) return;
        Set<String> allowed = getFilterableFields(dtoClass);
        for (SortCriteria s : sort) {
            String field = s.field();
            if (field == null || field.isBlank()) continue;
            String root = field.contains(".") ? field.split("\\.")[0] : field;
            if (!allowed.contains(root)) {
                throw new IllegalArgumentException("Sorting by '" + field + "' is not allowed for " + dtoClass.getSimpleName());
            }
        }
    }

    private static Set<String> getFilterableFields(Class<?> cls) {
        return CACHE.computeIfAbsent(cls, c -> {
            Set<String> result = new HashSet<>();
            Class<?> current = c;
            // Walk up the class hierarchy to include fields declared on superclasses
            while (current != null && current != Object.class) {
                for (Field f : current.getDeclaredFields()) {
                    if (f.isAnnotationPresent(Filterable.class)) {
                        result.add(f.getName());
                    }
                }
                current = current.getSuperclass();
            }
            return result;
        });
    }
}
