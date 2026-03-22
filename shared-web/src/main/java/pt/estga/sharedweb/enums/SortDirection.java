package pt.estga.sharedweb.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/**
 * Enum representing sort direction. Provides a case-insensitive JsonCreator
 * to accept values like "desc", "Desc" or "DESC" from JSON payloads.
 */
public enum SortDirection {
    ASC, DESC;

    @JsonCreator
    public static SortDirection from(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        return SortDirection.valueOf(v.toUpperCase(Locale.ROOT));
    }
}