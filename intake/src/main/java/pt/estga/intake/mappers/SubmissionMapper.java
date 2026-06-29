package pt.estga.intake.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.commoncore.interfaces.DivisionLookupClient;
import pt.estga.commoncore.models.DivisionRef;
import pt.estga.intake.dtos.SubmissionDto;
import pt.estga.intake.entities.MarkEvidenceSubmission;

@Component
@RequiredArgsConstructor
public class SubmissionMapper {

    private final DivisionLookupClient divisionLookupClient;

    public SubmissionDto toDto(MarkEvidenceSubmission s) {
        if (s == null) return null;
        String divisionName = resolveDivisionName(s.getDivisionCode());
        return new SubmissionDto(
                s.getId(),
                s.getStatus(),
                s.getSubmissionSource(),
                s.getSubmittedAt(),
                s.getLatitude(),
                s.getLongitude(),
                s.getUserNotes(),
                s.getOriginalMediaFileId(),
                s.getSubmittedById(),
                s.getDivisionCode(),
                divisionName
        );
    }

    private String resolveDivisionName(String code) {
        if (code == null || code.isBlank()) return null;
        return divisionLookupClient.findByCode(code)
                .map(DivisionRef::name)
                .orElse(null);
    }
}
