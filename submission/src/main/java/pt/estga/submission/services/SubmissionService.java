package pt.estga.submission.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.services.MarkEvidenceService;
import pt.estga.submission.entities.MarkEvidenceSubmission;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final MarkEvidenceService markEvidenceService;

    public void submit(
            MarkEvidenceSubmission submission,
            byte[] photoData,
            String photoFilename
    ) throws IOException {

        if (photoData == null || photoData.length == 0) {
            throw new IllegalArgumentException("Submission photo is required");
        }

    }
}
