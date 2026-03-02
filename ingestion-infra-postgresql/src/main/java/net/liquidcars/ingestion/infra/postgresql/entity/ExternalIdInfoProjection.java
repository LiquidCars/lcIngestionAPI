package net.liquidcars.ingestion.infra.postgresql.entity;

public interface ExternalIdInfoProjection {
    String getOwnerReference();
    String getDealerReference();
    String getChannelReference();
}