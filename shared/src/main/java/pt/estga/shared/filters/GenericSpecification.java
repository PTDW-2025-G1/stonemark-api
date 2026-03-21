package pt.estga.shared.filters;

import pt.estga.shared.filters.enums.LikeMode;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@RequiredArgsConstructor
@Slf4j
public class GenericSpecification<T> implements Specification<T> {

    private final FilterCriteria criteria;

    private final Object value;
    private final List<Object> values;

    public GenericSpecification(FilterCriteria criteria) {
        this.criteria = criteria;
        if (criteria.getValue() instanceof List<?> list) {
            this.values = new ArrayList<>(list);
            this.value = null;
        } else {
            this.value = criteria.getValue();
            this.values = null;
        }
    }

    @Override
    public Predicate toPredicate(@NonNull Root<T> root, @NonNull CriteriaQuery<?> query, @NonNull CriteriaBuilder cb) {

        if (!validateCriteria()) {
            throw new IllegalArgumentException(invalidFilterMessage("validation failed"));
        }

        Path<?> path = getPath(root, criteria.getField());
        if (path == null) {
            throw new IllegalArgumentException(invalidFilterMessage("invalid path"));
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

    private boolean validateCriteria() {
        if (criteria == null) return false;
        if (criteria.getField() == null || criteria.getField().isBlank()) return false;
        if (criteria.getOperator() == null) return false;

        return switch (criteria.getOperator()) {
            case IN, BETWEEN -> values != null && !values.isEmpty();
            case LIKE, EQ, NE, GT, LT, GTE, LTE -> value != null;
            default -> true;
        };
    }

    // ----------------------
    // CENTRALIZED CONVERSION
    // ----------------------

    @SuppressWarnings("unchecked")
    private <Y> Y convertSingle(Path<?> path, Object value) {
        if (value == null) return null;

        Class<?> type = path.getJavaType();

        // Enum handling
        if (type.isEnum()) {
            if (value instanceof String s) return convertEnum(type, s);
            if (type.isInstance(value)) return (Y) value;
            throw new IllegalArgumentException("Invalid enum value: " + value);
        }

        // String -> primitives
        switch (value) {
            case String s -> {
                return (Y) parseString(type, s);
            }

            // Number conversion with widening
            case Number num -> {
                return (Y) convertNumber(type, num);
            }

            // Boolean
            case Boolean b -> {
                if (type == Boolean.class || type == boolean.class) return (Y) b;
                if (type == String.class) return (Y) String.valueOf(b);
            }
            default -> {
                log.warn("Value of type {} cannot be directly converted to {}, attempting fallback", value.getClass().getSimpleName(), type.getSimpleName());
            }
        }

        if (type.isInstance(value)) return (Y) value;

        throw new IllegalArgumentException("Cannot convert value '" + value + "' to type " + type.getSimpleName());
    }

    private Object parseString(Class<?> type, String s) {
        return switch (type.getSimpleName()) {
            case "Integer", "int" -> Integer.valueOf(s);
            case "Long", "long" -> Long.valueOf(s);
            case "Short", "short" -> Short.valueOf(s);
            case "Byte", "byte" -> Byte.valueOf(s);
            case "Double", "double" -> Double.valueOf(s);
            case "Float", "float" -> Float.valueOf(s);
            case "Boolean", "boolean" -> Boolean.valueOf(s);
            case "String" -> s;
            default -> throw new IllegalArgumentException("Cannot convert String '" + s + "' to type " + type.getSimpleName());
        };
    }

    private Object convertNumber(Class<?> type, Number num) {
        return switch (type.getSimpleName()) {
            case "Integer", "int" -> num.intValue();
            case "Long", "long" -> num.longValue();
            case "Short", "short" -> num.shortValue();
            case "Byte", "byte" -> num.byteValue();
            case "Double", "double" -> num.doubleValue();
            case "Float", "float" -> num.floatValue();
            default -> throw new IllegalArgumentException("Cannot convert number '" + num + "' to type " + type.getSimpleName());
        };
    }

    @SuppressWarnings("unchecked")
    private <Y extends Enum<Y>> Y convertEnum(Class<?> enumType, String name) {
        try {
            return Enum.valueOf((Class<Y>) enumType, name);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid enum value: " + name, ex);
        }
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
        return input.replace(esc, esc + esc).replace("%", esc + "%").replace("_", esc + "_");
    }

    private Predicate inPredicate(Path<?> path, Object value) {
        Objects.requireNonNull(value, "IN operator requires a non-null value");
        if (!(value instanceof List<?> list)) throw new IllegalArgumentException("IN operator requires a list of values");
        for (Object e : list) {
            if (e == null) throw new IllegalArgumentException("IN list contains null element");
        }
        List<?> converted = convertList(path, list);
        return path.in(converted);
    }

    @SuppressWarnings("unchecked")
    private <Y extends Comparable<? super Y>> Predicate betweenPredicate(CriteriaBuilder cb, Path<?> path, Object value) {
        Objects.requireNonNull(value, "BETWEEN operator requires a non-null value");
        if (!(value instanceof List<?> list) || list.size() != 2) {
            throw new IllegalArgumentException("BETWEEN requires list of size 2");
        }

        List<Y> converted = convertList(path, list);
        Y start = converted.get(0);
        Y end = converted.get(1);

        if (start.compareTo(end) > 0) throw new IllegalArgumentException("BETWEEN start value must be less than or equal to end value");
        return cb.between((Path<Y>) path, start, end);
    }

    // ----------------------
    // PATH / JOIN
    // ----------------------

    private Path<?> getPath(Root<T> root, String field) {
        if (!field.contains(".")) return root.get(field);

        String[] parts = field.split("\\.");
        Path<?> path = root;

        for (String part : parts) {
            if (path instanceof From<?, ?> from) {
                path = from.join(part, JoinType.LEFT);
            } else {
                path = path.get(part);
            }
            if (path == null) {
                throw new IllegalArgumentException("Invalid path segment: " + part);
            }
        }

        return path;
    }

    private String invalidFilterMessage(String reason) {
        String field = criteria == null ? "<null>" : criteria.getField();
        String op = criteria == null ? "<null>" : String.valueOf(criteria.getOperator());
        Object val = criteria == null ? null : criteria.getValue();
        String valStr = val == null ? "null" : val.toString();
        return "Invalid filter (" + reason + ") - field=" + field + ", operator=" + op + ", value=" + valStr;
    }
}
