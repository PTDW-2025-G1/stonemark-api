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

    private static final ThreadLocal<Map<Root<?>, Map<String, Join<?, ?>>>> THREAD_LOCAL_JOIN_CACHE =
            ThreadLocal.withInitial(WeakHashMap::new);

    @Override
    public Predicate toPredicate(@NonNull Root<T> root, @NonNull CriteriaQuery<?> query, @NonNull CriteriaBuilder cb) {
        Map<Root<?>, Map<String, Join<?, ?>>> perThread = THREAD_LOCAL_JOIN_CACHE.get();
        try {
            if (!validateCriteria()) {
                throw new IllegalArgumentException(invalidFilterMessage("validation failed"));
            }

            Path<?> path = getPath(root, criteria.getField());
            if (path == null) {
                throw new IllegalArgumentException(invalidFilterMessage("invalid path"));
            }

            Predicate predicate = switch (criteria.getOperator()) {
                case EQ -> cb.equal(path, convertSingle(path, criteria.getValue()));
                case NE -> cb.notEqual(path, convertSingle(path, criteria.getValue()));
                case GT -> comparison(cb, path, criteria.getValue(), ComparisonOperator.GT);
                case LT -> comparison(cb, path, criteria.getValue(), ComparisonOperator.LT);
                case GTE -> comparison(cb, path, criteria.getValue(), ComparisonOperator.GTE);
                case LTE -> comparison(cb, path, criteria.getValue(), ComparisonOperator.LTE);
                case LIKE -> likePredicate(cb, path, criteria.getValue());
                case IN -> inPredicate(cb, path, criteria.getValue());
                case BETWEEN -> betweenPredicate(cb, path, criteria.getValue());
                case IS_NULL -> cb.isNull(path);
                case IS_NOT_NULL -> cb.isNotNull(path);
                default -> throw new UnsupportedOperationException("Operator not supported: " + criteria.getOperator());
            };

            return Objects.requireNonNull(predicate, "Predicate creation failed");

        } finally {
            try {
                perThread.remove(root);
            } catch (Exception ignored) {}
            if (perThread.isEmpty()) THREAD_LOCAL_JOIN_CACHE.remove();
        }
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

        Object value = criteria.getValue();
        return switch (criteria.getOperator()) {
            case IN, BETWEEN -> value instanceof List<?> list && !list.isEmpty();
            case LIKE, EQ, NE, GT, LT, GTE, LTE -> value != null;
            default -> true;
        };
    }

    private boolean isEnumField(Path<?> path) {
        return path != null && path.getJavaType().isEnum();
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
            if (value instanceof String s) return (Y) convertEnum(type, s);
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
    private Object convertEnum(Class<?> enumType, String name) {
        try {
            return Enum.valueOf((Class<? extends Enum>) enumType, name);
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
        Objects.requireNonNull(value, "LIKE operator requires a non-null value");
        String str = (String) value;
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

    private Predicate inPredicate(CriteriaBuilder cb, Path<?> path, Object value) {
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
        Map<Root<?>, Map<String, Join<?, ?>>> perThread = THREAD_LOCAL_JOIN_CACHE.get();
        Map<String, Join<?, ?>> joinCache = perThread.computeIfAbsent(root, r -> new HashMap<>());
        return getPath(root, field, joinCache);
    }

    private Path<?> getPath(Root<T> root, String field, Map<String, Join<?, ?>> joinCache) {
        if (!field.contains(".")) return root.get(field);
        String[] parts = field.split("\\.");
        Path<?> path = root;
        for (int i = 0; i < parts.length; i++) {
            String key = String.join(".", Arrays.copyOfRange(parts, 0, i + 1));
            final String part = parts[i];
            if (path instanceof Root<?> r) path = joinCache.computeIfAbsent(key, k -> r.join(part, JoinType.LEFT));
            else if (path instanceof Join<?, ?> j) path = joinCache.computeIfAbsent(key, k -> j.join(part, JoinType.LEFT));
            else path = path.get(part);
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
