package pt.estga.processing.testutils;

import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.CandidateEvidence;

import java.util.UUID;
import java.util.HexFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class TestBuilders {

    private TestBuilders() {}

    public static CandidateEvidence candidate(UUID evidenceId, Long occurrenceId, double similarity) {
        return new CandidateEvidence(evidenceId, occurrenceId, similarity);
    }

    public static Mark mark(Long id) {
        return Mark.builder().id(id).build();
    }

    public static UUID uuid(String literal) {
        // Strict parsing only: require standard dashed UUID string format.
        return UUID.fromString(literal);
    }

    /**
     * Create a UUID from a compact hex literal.
     * Accepts either 16-hex (lsb only, msb=0) or 32-hex (msb+lsb).
     * Throws IllegalArgumentException for other lengths.
     */
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
