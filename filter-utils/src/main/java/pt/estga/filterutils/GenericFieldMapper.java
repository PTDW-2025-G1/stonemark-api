package pt.estga.filterutils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic JPA Field Mapper.
 * Validates entity fields, including nested paths, using the JPA metamodel.
 * Caches allowed fields per entity for performance.
 */
@Component
public class GenericFieldMapper {

    private final EntityManager entityManager;
    private final Map<Class<?>, Set<String>> allowedFieldsCache = new ConcurrentHashMap<>();

    public GenericFieldMapper(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager cannot be null");
    }

    /**
     * Returns the canonical field path if it exists, otherwise throws.
     * Example: "roles.name" -> "roles.name"
     */
    public String mapOrThrow(String field, Class<?> entityClass) {
        if (!isAllowed(field, entityClass)) {
            throw new IllegalArgumentException("Field not allowed: " + field);
        }
        return field;
    }

    /**
     * Returns true if the field exists on the entity (including nested paths)
     */
    public boolean isAllowed(String field, Class<?> entityClass) {
        Objects.requireNonNull(entityClass, "entityClass cannot be null");
        Objects.requireNonNull(field, "field cannot be null or blank");
        if (field.isBlank()) return false;

        // Compute allowed fields if not cached
        Set<String> allowed = allowedFieldsCache.computeIfAbsent(entityClass, cls -> buildAllowedFields(cls));
        return allowed.contains(field);
    }

    // -------------------
    // INTERNAL
    // -------------------

    private Set<String> buildAllowedFields(Class<?> entityClass) {
        Metamodel metamodel = entityManager.getMetamodel();
        EntityType<?> entityType = metamodel.entity(entityClass);
        Set<String> result = new HashSet<>();
        collectFields(entityType, "", result);
        return result;
    }

    private void collectFields(ManagedType<?> type, String prefix, Set<String> collector) {
        for (Attribute<?, ?> attr : type.getAttributes()) {
            String path = prefix.isEmpty() ? attr.getName() : prefix + "." + attr.getName();

            if (attr instanceof SingularAttribute<?, ?> sa) {
                if (sa.getType() instanceof ManagedType<?> nested) {
                    collectFields(nested, path, collector);
                } else {
                    collector.add(path);
                }
            } else if (attr instanceof PluralAttribute<?, ?, ?> pa) {
                if (pa.getElementType() instanceof ManagedType<?> nested) {
                    collectFields(nested, path, collector);
                } else {
                    collector.add(path);
                }
            }
        }
    }
}