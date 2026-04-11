package pt.estga.processing.testutils;

import java.util.UUID;
import java.util.HexFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Test-only helper: build UUIDs from compact hex literals.
 * Kept separate from production utilities to avoid accidental reliance on
 * test-specific encoding semantics.
 */
public final class HexUuid {
    private HexUuid() {}

    public static UUID uuidFromHex(String hexLiteral) {
        String hex = hexLiteral.trim();
        HexFormat hf = HexFormat.of();
        if (hex.length() == 16) {
            byte[] bytes = hf.parseHex(hex);
            if (bytes.length != 8) throw new IllegalArgumentException("Invalid 16-hex literal: " + hexLiteral);
            long lsb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getLong();
            return new UUID(0L, lsb);
        }
        if (hex.length() == 32) {
            byte[] bytes = hf.parseHex(hex);
            if (bytes.length != 16) throw new IllegalArgumentException("Invalid 32-hex literal: " + hexLiteral);
            ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            long msb = bb.getLong();
            long lsb = bb.getLong();
            return new UUID(msb, lsb);
        }
        throw new IllegalArgumentException("Invalid hex UUID literal: " + hexLiteral);
    }
}
