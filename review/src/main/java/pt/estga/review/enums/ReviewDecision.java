package pt.estga.review.enums;

import lombok.Getter;

/**
 * Review decision with stable integer codes.
 * <p>
 * Explicit codes are preferred over relying on {@link #ordinal()} which may change if constants are reordered.
 */
@Getter
public enum ReviewDecision {
    APPROVED(1),
    REJECTED(2),
    NO_MATCH(3),
    FLAGGED(4);

    private final int code;

    ReviewDecision(int code) {
        this.code = code;
    }

    /**
     * Returns the enum constant corresponding to the provided code.
     *
     * @throws IllegalArgumentException if the code is unknown
     */
    public static ReviewDecision fromCode(int code) {
        for (ReviewDecision d : values()) {
            if (d.code == code) return d;
        }
        throw new IllegalArgumentException("Unknown ReviewDecision code: " + code);
    }
}