package net.liquidcars.ingestion.infra.output.kafka.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ParticipantAddressMsg implements Serializable {
    private AddressTypeMsg type;
    private PostalAddressMsg address;
}
