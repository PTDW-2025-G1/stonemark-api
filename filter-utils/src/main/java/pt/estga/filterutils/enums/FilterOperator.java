package pt.estga.filterutils.enums;

public enum FilterOperator {
    EQ,         // ==
    NE,         // !=
    GT,         // >
    LT,         // <
    GTE,        // >=
    LTE,        // <=
    LIKE,       // %value%
    IN,         // list
    BETWEEN,    // range
    IS_NULL,    // is null
    IS_NOT_NULL // is not null
}
