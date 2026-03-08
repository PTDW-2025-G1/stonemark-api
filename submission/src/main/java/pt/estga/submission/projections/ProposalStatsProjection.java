package pt.estga.submission.projections;

public interface ProposalStatsProjection {
    long getAccepted();
    long getUnderReview();
    long getRejected();
}
