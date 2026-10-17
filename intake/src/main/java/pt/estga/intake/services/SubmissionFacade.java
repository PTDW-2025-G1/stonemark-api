package pt.estga.intake.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import pt.estga.shared.core.utils.SecurityUtils;
import pt.estga.file.api.FileStorageOperations;
import pt.estga.file.api.StagedFileRecord;
import pt.estga.intake.dtos.SubmissionDto;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionSource;
import pt.estga.intake.mappers.SubmissionMapper;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;

import java.io.IOException;
import java.util.Optional;

/**
 * Entry point for creating submissions from external channels (web, chatbot).
 *
 * <p>Deliberately not transactional: slow external work (file staging) happens
 * outside the database transaction that persists the submission.
 * The database write is delegated to {@link MarkEvidenceSubmissionSubmitService},
 * which stores the media object and the submission row in one transaction and
 * compensates object storage on rollback.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionFacade {

    private final FileStorageOperations fileStorage;
    private final MarkEvidenceSubmissionSubmitService submitService;
    private final MarkEvidenceSubmissionRepository submissionRepository;
    private final SubmissionMapper submissionMapper;

    /**
     * Submits evidence from the public web flow.
     *
     * @param idempotencyKey optional client-supplied key; replaying the same key
     *                       returns the previously created submission instead of
     *                       creating a duplicate.
     */
    public SubmissionDto submitFromWeb(MultipartFile file, Double latitude, Double longitude,
                                       String notes, String idempotencyKey) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A photo file is required");
        }

        String key = normalizeKey(idempotencyKey);
        if (key != null) {
            Optional<MarkEvidenceSubmission> existing = submissionRepository.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                log.info("Idempotent replay for key {} -> submission {}", key, existing.get().getId());
                return submissionMapper.toDto(existing.get());
            }
        }

        StagedFileRecord staged = fileStorage.stage(file.getInputStream(), file.getOriginalFilename());

        MarkEvidenceSubmission submission = MarkEvidenceSubmission.builder()
                .latitude(latitude)
                .longitude(longitude)
                .userNotes(notes)
                .submissionSource(SubmissionSource.WEB_APP)
                .idempotencyKey(key)
                .build();
        SecurityUtils.getCurrentUserId().ifPresent(submission::setSubmittedById);

        try {
            Long id = submitService.submit(submission, staged.id(), file.getOriginalFilename());
            return submissionMapper.toDto(submissionRepository.findById(id).orElseThrow());
        } catch (DataIntegrityViolationException duplicate) {
            discardStagedQuietly(staged);
            if (key != null) {
                // Concurrent request won the race on the unique idempotency key.
                return submissionRepository.findByIdempotencyKey(key)
                        .map(submissionMapper::toDto)
                        .orElseThrow(() -> duplicate);
            }
            throw duplicate;
        } catch (RuntimeException e) {
            discardStagedQuietly(staged);
            throw e;
        }
    }

    private void discardStagedQuietly(StagedFileRecord staged) {
        try {
            fileStorage.discardStaged(staged.id(), staged.originalFilename());
        } catch (Exception e) {
            log.warn("Failed to clean up staged file {}: {}", staged.id(), e.getMessage());
        }
    }

    private static String normalizeKey(String key) {
        return (key == null || key.isBlank()) ? null : key.trim();
    }
}
