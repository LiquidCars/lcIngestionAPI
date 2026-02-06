package net.liquidcars.ingestion.infra.output.kafka.model;

import java.io.Serializable;

public enum EnvironmentTypeMsg implements Serializable {
    All,
    DEV,
    UAT,
    PROD,
    TEST
}
