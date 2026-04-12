package pt.estga.processing.testutils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Test-only deterministic UUID generator from 16-hex (64-bit) or 32-hex (128-bit) strings.
 * This utility is test-scoped only and must not be used from production code.
 */
public final class TestUuidUtil {
    private TestUuidUtil() {}

    /**
     * Create a UUID from a hex string.
     * - 16 hex chars => interpreted as 64-bit LSB, MSB = 0
     * - 32 hex chars => interpreted as 128-bit (MSB|LSB)
     * Throws IllegalArgumentException for other lengths or null.
     */
    public static UUID uuidFromHex(String hex) {
        if (hex == null) throw new IllegalArgumentException("hex must not be null");
        hex = hex.trim();
        if (hex.length() == 16) {
            byte[] bytes = HexFormat.of().parseHex(hex);
            ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            long lsb = bb.getLong();
            return new UUID(0L, lsb);
        } else if (hex.length() == 32) {
            byte[] bytes = HexFormat.of().parseHex(hex);
            ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            long msb = bb.getLong();
            long lsb = bb.getLong();
            return new UUID(msb, lsb);
        } else {
            throw new IllegalArgumentException("Expected 16 or 32 hex chars, got: " + hex);
        }
    }
}
