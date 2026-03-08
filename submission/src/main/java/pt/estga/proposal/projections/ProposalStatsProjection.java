package pt.estga.proposal.projections;

public interface ProposalStatsProjection {
    long getAccepted();
    long getUnderReview();
    long getRejected();
}
