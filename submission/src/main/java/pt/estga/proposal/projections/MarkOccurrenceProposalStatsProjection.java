package pt.estga.proposal.projections;

public interface MarkOccurrenceProposalStatsProjection {
    long getAccepted();
    long getUnderReview();
    long getRejected();
}
