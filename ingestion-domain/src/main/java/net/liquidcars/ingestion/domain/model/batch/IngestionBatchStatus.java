package net.liquidcars.ingestion.domain.model.batch;

public enum IngestionBatchStatus {
    COMPLETED,
    STARTING,
    STARTED,
    STOPPING,
    STOPPED,
    FAILED,
    ABANDONED,
    UNKNOWN;
}
