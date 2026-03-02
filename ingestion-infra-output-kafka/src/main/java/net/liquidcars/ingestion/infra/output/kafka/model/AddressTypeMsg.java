package net.liquidcars.ingestion.infra.output.kafka.model;

import java.io.Serializable;

public enum AddressTypeMsg implements Serializable {
    P_HOME,
    B_RETAIL,
    B_PICKUP,
    B_EXPERTISE
}
