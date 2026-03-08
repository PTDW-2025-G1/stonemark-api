package pt.estga.submission.projections;

public interface MarkOccurrenceProposalStatsProjection {
    long getAccepted();
    long getUnderReview();
    long getRejected();
}
