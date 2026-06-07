package pt.estga.intake.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pt.estga.fileapi.FileStorageOperations;
import pt.estga.intake.dtos.SubmissionDto;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionSource;
import pt.estga.intake.mappers.SubmissionMapper;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.sharedcore.utils.SecurityUtils;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionFacade {

    private final FileStorageOperations fileStorage;
    private final MarkEvidenceSubmissionSubmitService submitService;
    private final MarkEvidenceSubmissionRepository submissionRepository;

    @Transactional
    public SubmissionDto submitFromWeb(MultipartFile file, Double latitude, Double longitude, String notes) throws IOException {
        var staged = fileStorage.stage(file.getInputStream(), file.getOriginalFilename());

        var submission = MarkEvidenceSubmission.builder()
                .latitude(latitude)
                .longitude(longitude)
                .userNotes(notes)
                .submissionSource(SubmissionSource.WEB_APP)
                .build();

        SecurityUtils.getCurrentUserId().ifPresent(submission::setSubmittedById);

        submitService.submit(submission, staged.id(), file.getOriginalFilename());

        return SubmissionMapper.toDto(submissionRepository.findById(submission.getId()).orElseThrow());
    }

    public void submitStaged(MarkEvidenceSubmission submission, UUID stagedFileId,
                              String photoFilename, Long domainUserId, SubmissionSource source) {
        if (domainUserId != null) {
            submission.setSubmittedById(domainUserId);
        }
        if (submission.getSubmissionSource() == null) {
            submission.setSubmissionSource(source != null ? source : SubmissionSource.OTHER);
        }
        submitService.submit(submission, stagedFileId, photoFilename);
    }
}
