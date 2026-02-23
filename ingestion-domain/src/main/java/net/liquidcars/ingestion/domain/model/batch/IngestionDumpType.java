package net.liquidcars.ingestion.domain.model.batch;

public enum IngestionDumpType {
    INCREMENTAL, // Apply payload to modify inventory
    REPLACEMENT // Replace inventory with payload
}
