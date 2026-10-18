package pt.estga.mark;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import pt.estga.mark.api.MarkEvidenceQueryService;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Exercises Spring Data's vector search against a real PostgreSQL with the
 * {@code vector} extension, so the cosine operator translation and the Hibernate
 * Vector mapping are verified end to end rather than assumed from an in-memory run.
 *
 * <p>Skipped automatically when no Docker daemon is available, so the default build
 * does not depend on Docker while CI and local development do exercise it.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class MarkEvidenceVectorSearchTest {

    private static final int DIM = 384;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17");

    @Autowired
    private MarkEvidenceQueryService queryService;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void createSchema() {
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbc.execute("DROP TABLE IF EXISTS mark_evidence");
        jdbc.execute("""
                CREATE TABLE mark_evidence (
                    id uuid PRIMARY KEY,
                    file_id uuid,
                    submission_id uuid,
                    embedding vector(384),
                    embedding_model varchar(64),
                    embedding_version varchar(64),
                    embedding_dim integer,
                    occurrence_id uuid,
                    deleted_at timestamp with time zone,
                    created_at timestamp with time zone NOT NULL,
                    created_by_id uuid,
                    last_modified_at timestamp with time zone,
                    modified_by_id uuid
                )
                """);
    }

    @Test
    @DisplayName("ranks by cosine similarity, and skips soft-deleted rows")
    void ranksBySimilarity() {
        float[] query = unitVector(0);
        UUID occurrence = UUID.randomUUID();
        UUID identical = UUID.randomUUID();
        UUID orthogonal = UUID.randomUUID();
        UUID deleted = UUID.randomUUID();

        insert(identical, occurrence, query, null);
        insert(orthogonal, occurrence, unitVector(1), null);
        insert(deleted, occurrence, query, Instant.now());

        List<MarkEvidenceDistanceDto> hits = queryService.findTopKSimilar(query, 10, 0.0);

        assertThat(hits).extracting(MarkEvidenceDistanceDto::id).containsExactly(identical, orthogonal);
        assertThat(hits.get(0).similarity()).isCloseTo(1.0, within(1e-4));
        assertThat(hits.get(1).similarity()).isCloseTo(0.0, within(1e-4));
    }

    @Test
    @DisplayName("excludes candidates below the similarity threshold and honours the result limit")
    void appliesThresholdAndLimit() {
        float[] query = unitVector(0);
        UUID occurrence = UUID.randomUUID();
        UUID identical = UUID.randomUUID();
        UUID orthogonal = UUID.randomUUID();

        insert(identical, occurrence, query, null);
        insert(orthogonal, occurrence, unitVector(1), null);

        assertThat(queryService.findTopKSimilar(query, 10, 0.5))
                .extracting(MarkEvidenceDistanceDto::id).containsExactly(identical);
        assertThat(queryService.findTopKSimilar(query, 1, 0.0))
                .extracting(MarkEvidenceDistanceDto::id).containsExactly(identical);
    }

    private void insert(UUID id, UUID occurrenceId, float[] embedding, Instant deletedAt) {
        jdbc.update("""
                        INSERT INTO mark_evidence (id, occurrence_id, embedding, deleted_at, created_at)
                        VALUES (?, ?, CAST(? AS vector), ?, ?)
                        """,
                id, occurrenceId, toLiteral(embedding),
                deletedAt == null ? null : Timestamp.from(deletedAt), Timestamp.from(Instant.now()));
    }

    private static float[] unitVector(int axis) {
        float[] vector = new float[DIM];
        vector[axis] = 1f;
        return vector;
    }

    private static String toLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
