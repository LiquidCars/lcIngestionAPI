package net.liquidcars.ingestion.application.service.parser.model.JSON;

import java.io.Serializable;

public enum EnvironmentTypeJSONModel implements Serializable {
    All,
    DEV,
    UAT,
    PROD,
    TEST
}
