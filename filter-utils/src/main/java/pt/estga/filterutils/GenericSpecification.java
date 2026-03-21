package pt.estga.filterutils;

import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import pt.estga.filterutils.enums.FilterOperator;
import pt.estga.filterutils.enums.LikeMode;
import pt.estga.filterutils.models.FilterCriteria;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

@Slf4j
public class GenericSpecification<T> implements Specification<T> {

    private final FilterCriteria criteria;
    private final Object value;
    private final List<Object> values;

    private static final Map<Class<?>, Function<String, ?>> TYPE_REGISTRY = new HashMap<>();

    static {
        TYPE_REGISTRY.put(Integer.class, Integer::valueOf);
        TYPE_REGISTRY.put(int.class, Integer::valueOf);
        TYPE_REGISTRY.put(Long.class, Long::valueOf);
        TYPE_REGISTRY.put(long.class, Long::valueOf);
        TYPE_REGISTRY.put(Short.class, Short::valueOf);
        TYPE_REGISTRY.put(short.class, Short::valueOf);
        TYPE_REGISTRY.put(Byte.class, Byte::valueOf);
        TYPE_REGISTRY.put(byte.class, Byte::valueOf);
        TYPE_REGISTRY.put(Double.class, Double::valueOf);
        TYPE_REGISTRY.put(double.class, Double::valueOf);
        TYPE_REGISTRY.put(Float.class, Float::valueOf);
        TYPE_REGISTRY.put(float.class, Float::valueOf);
        TYPE_REGISTRY.put(Boolean.class, Boolean::valueOf);
        TYPE_REGISTRY.put(boolean.class, Boolean::valueOf);
        TYPE_REGISTRY.put(String.class, Function.identity());
        TYPE_REGISTRY.put(Character.class, s -> !s.isEmpty() ? s.charAt(0) : null);
        TYPE_REGISTRY.put(char.class, s -> !s.isEmpty() ? s.charAt(0) : null);
        TYPE_REGISTRY.put(LocalDate.class, LocalDate::parse);
        TYPE_REGISTRY.put(Instant.class, Instant::parse);
        TYPE_REGISTRY.put(BigDecimal.class, BigDecimal::new);
        TYPE_REGISTRY.put(BigInteger.class, BigInteger::new);
        TYPE_REGISTRY.put(UUID.class, UUID::fromString);
    }

    public GenericSpecification(FilterCriteria criteria) {
        this.criteria = Objects.requireNonNull(criteria, "FilterCriteria cannot be null");

        if (criteria.getValue() instanceof List<?> list) {
            this.values = List.copyOf(list); // immutable copy
            this.value = null;
        } else {
            this.value = criteria.getValue();
            this.values = null;
        }
    }

    @Override
    public Predicate toPredicate(
            @NonNull Root<T> root,
            CriteriaQuery<?> query,
            @NonNull CriteriaBuilder cb
    ) {
        Path<?> path = getPath(root, criteria.getField());
        if (path == null) {
            throw new IllegalArgumentException(invalidFilterMessage());
        }

        if (value == null && !criteria.getOperator().equals(FilterOperator.IS_NULL) && !criteria.getOperator().equals(FilterOperator.IS_NOT_NULL)) {
            throw new IllegalArgumentException("Null value not allowed for operator: " + criteria.getOperator());
        }

        Predicate predicate = switch (criteria.getOperator()) {
            case EQ -> cb.equal(path, convertSingle(path, value));
            case NE -> cb.notEqual(path, convertSingle(path, value));
            case GT -> comparison(cb, path, value, ComparisonOperator.GT);
            case LT -> comparison(cb, path, value, ComparisonOperator.LT);
            case GTE -> comparison(cb, path, value, ComparisonOperator.GTE);
            case LTE -> comparison(cb, path, value, ComparisonOperator.LTE);
            case LIKE -> likePredicate(cb, path, value);
            case IN -> inPredicate(path, values);
            case BETWEEN -> betweenPredicate(cb, path, values);
            case IS_NULL -> cb.isNull(path);
            case IS_NOT_NULL -> cb.isNotNull(path);
        };

        log.debug("Created predicate for field: {}, operator: {}, value: {}", criteria.getField(), criteria.getOperator(), criteria.getValue());

        return Objects.requireNonNull(predicate, "Predicate creation failed");
    }

    private enum ComparisonOperator { GT, LT, GTE, LTE }

    @SuppressWarnings("unchecked")
    private <Y extends Comparable<? super Y>> Predicate comparison(CriteriaBuilder cb, Path<?> path, Object value, ComparisonOperator op) {
        Y val = convertSingle(path, value);
        Path<Y> p = (Path<Y>) path;
        return switch (op) {
            case GT -> cb.greaterThan(p, val);
            case LT -> cb.lessThan(p, val);
            case GTE -> cb.greaterThanOrEqualTo(p, val);
            case LTE -> cb.lessThanOrEqualTo(p, val);
        };
    }


    // ----------------------
    // CENTRALIZED CONVERSION
    // ----------------------

    @SuppressWarnings("unchecked")
    private <Y> Y convertSingle(Path<?> path, Object value) {
        if (value == null) return null;

        Class<?> type = path.getJavaType();
        Function<String, ?> converter = TYPE_REGISTRY.get(type);

        switch (value) {
            case String s when converter != null -> {
                return (Y) converter.apply(s);
            }


            // Handle enums provided as strings
            case String s when type.isEnum() -> {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Y enumVal = (Y) Enum.valueOf((Class<Enum>) type, s);
                return enumVal;
            }


            // Allow numeric widening when value is a Number
            case Number n -> {
                if (type == Long.class || type == long.class) return (Y) Long.valueOf(n.longValue());
                if (type == Integer.class || type == int.class) return (Y) Integer.valueOf(n.intValue());
                if (type == Short.class || type == short.class) return (Y) Short.valueOf(n.shortValue());
                if (type == Byte.class || type == byte.class) return (Y) Byte.valueOf(n.byteValue());
                if (type == Double.class || type == double.class) return (Y) Double.valueOf(n.doubleValue());
                if (type == Float.class || type == float.class) return (Y) Float.valueOf(n.floatValue());
                if (type == BigDecimal.class) return (Y) BigDecimal.valueOf(n.doubleValue());
                if (type == BigInteger.class) return (Y) BigInteger.valueOf(n.longValue());
            }
            default -> {
            }
        }

        if (type.isInstance(value)) return (Y) value;

        throw new IllegalArgumentException("Cannot convert value '" + value + "' to type " + type.getSimpleName());
    }

    private <Y> List<Y> convertList(Path<?> path, List<?> list) {
        List<Y> result = new ArrayList<>(list.size());
        for (Object v : list) {
            result.add(convertSingle(path, v));
        }
        return result;
    }

    // ----------------------
    // PREDICATES
    // ----------------------

    private Predicate likePredicate(CriteriaBuilder cb, Path<?> path, Object value) {
        if (!(value instanceof String str)) {
            throw new IllegalArgumentException("LIKE requires string value");
        }
        LikeMode mode = criteria.getLikeMode() != null ? criteria.getLikeMode() : LikeMode.CONTAINS;
        char escapeChar = '\\';
        String escaped = escapeLike(str, escapeChar);
        String pattern = switch (mode) {
            case STARTS_WITH -> escaped + "%";
            case ENDS_WITH -> "%" + escaped;
            case CONTAINS -> "%" + escaped + "%";
        };
        if (criteria.isCaseSensitive()) return cb.like(path.as(String.class), pattern, escapeChar);
        return cb.like(cb.lower(path.as(String.class)), pattern.toLowerCase(), escapeChar);
    }

    private String escapeLike(String input, char escapeChar) {
        String esc = String.valueOf(escapeChar);
        return input.replace(esc, esc + esc)
                    .replace("%", esc + "%")
                    .replace("_", esc + "_")
                    .replace("[", esc + "[")
                    .replace("]", esc + "]");
    }

    private Predicate inPredicate(Path<?> path, List<?> values) {
        Objects.requireNonNull(values, "IN operator requires a non-null list of values");
        for (Object e : values) {
            if (e == null) throw new IllegalArgumentException("IN list contains null element");
        }
        List<?> converted = convertList(path, values);
        return path.in(converted);
    }

    // Updated betweenPredicate to auto-swap start and end values
    @SuppressWarnings("unchecked")
    private <Y extends Comparable<? super Y>> Predicate betweenPredicate(CriteriaBuilder cb, Path<?> path, List<?> values) {
        Objects.requireNonNull(values, "BETWEEN operator requires a non-null list of values");
        if (values.size() != 2) {
            throw new IllegalArgumentException("BETWEEN requires list of size 2");
        }

        List<Y> converted = convertList(path, values);
        Y start = converted.get(0);
        Y end = converted.get(1);

        if (start.compareTo(end) > 0) {
            Y temp = start;
            start = end;
            end = temp;
        }

        return cb.between((Path<Y>) path, start, end);
    }

    // ----------------------
    // PATH / JOIN
    // ----------------------

    private Path<?> getPath(Root<T> root, String field) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("Field for path navigation cannot be null or blank");
        }

        // Trim whitespace at point-of-use to tolerate user input like "firstName "
        String trimmed = field.trim();
        String[] parts = trimmed.split("\\.");
        Path<?> path = root;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            // If it's not the last part, we MUST be able to join/navigate
            if (i < parts.length - 1) {
                if (path instanceof From<?, ?> from) {
                    path = from.join(part, criteria.getJoinType());
                } else {
                    path = path.get(part);
                }
            } else {
                // Last part: just get the attribute
                path = path.get(part);
            }
        }
        return path;
    }

    private String invalidFilterMessage() {
        String field = criteria == null ? "<null>" : criteria.getField();
        String op = criteria == null ? "<null>" : String.valueOf(criteria.getOperator());
        Object val = criteria == null ? null : criteria.getValue();
        String valStr = val == null ? "null" : val.toString();
        return "Invalid filter - field=" + field + ", operator=" + op + ", value=" + valStr;
    }
}
