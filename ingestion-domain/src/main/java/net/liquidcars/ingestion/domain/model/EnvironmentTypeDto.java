package net.liquidcars.ingestion.domain.model;

import java.io.Serializable;

public enum EnvironmentTypeDto implements Serializable {
    All,
    DEV,
    UAT,
    PROD,
    TEST
}