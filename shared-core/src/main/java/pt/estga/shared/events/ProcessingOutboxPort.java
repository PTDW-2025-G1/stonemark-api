package pt.estga.shared.events;

public interface ProcessingOutboxPort {
    void enqueue(Long submissionId);
}
