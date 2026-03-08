package pt.estga.proposal.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.dtos.MonumentRequestDto;
import pt.estga.content.entities.Monument;
import pt.estga.content.mappers.MonumentMapper;
import pt.estga.content.services.MonumentService;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.repositories.MarkOccurrenceProposalRepository;
import pt.estga.shared.exceptions.ResourceNotFoundException;
import pt.estga.territory.dtos.GeocodingResultDto;
import pt.estga.territory.services.ReverseGeocodingService;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonumentCreationService {

    private final MonumentService monumentService;
    private final ReverseGeocodingService reverseGeocodingService;
    private final MarkOccurrenceProposalRepository proposalRepo;
    private final MonumentMapper monumentMapper;

    /**
     * Creates a monument from the proposal data.
     * This should be called by a moderator during the approval process.
     */
    @Transactional
    public Monument createMonumentFromProposal(Long proposalId, MonumentRequestDto requestDto) {
        MarkOccurrenceProposal proposal = proposalRepo.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        if (proposal.getExistingMonument() != null) {
            return proposal.getExistingMonument();
        }

        Monument monument = monumentMapper.toEntity(requestDto);
        log.info("Creating new monument '{}' from proposal ID: {}", monument.getName(), proposalId);

        // Ensure lat/long from proposal are used if not provided in the monument object (though they should be)
        if (monument.getLatitude() == null) monument.setLatitude(proposal.getLatitude());
        if (monument.getLongitude() == null) monument.setLongitude(proposal.getLongitude());
        
        monument.setActive(true); // Active as it is being created by a moderator

        Monument savedMonument = monumentService.create(monument);
        
        proposal.setExistingMonument(savedMonument);
        proposalRepo.save(proposal);
        
        log.info("Created new monument with ID: {}", savedMonument.getId());
        return savedMonument;
    }

    public GeocodingResultDto getAutofillData(Long proposalId) {
        MarkOccurrenceProposal proposal = proposalRepo.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
        return getAutofillData(proposal);
    }
    
    public GeocodingResultDto getAutofillData(MarkOccurrenceProposal proposal) {
         if (proposal.getLatitude() != null && proposal.getLongitude() != null) {
            try {
                return reverseGeocodingService.reverseGeocode(proposal.getLatitude(), proposal.getLongitude());
            } catch (Exception e) {
                log.error("Failed to fetch geocoding info", e);
            }
        }
        return null;
    }
}
