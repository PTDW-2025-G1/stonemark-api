package pt.estga.intake.enums;

/**
 * Lifecycle of a submission. Processing progress lives in the processing module,
 * not here: a submission is received, then either kept or rejected.
 */
public enum SubmissionStatus {
    RECEIVED,
    PROCESSED,
    REJECTED
}
