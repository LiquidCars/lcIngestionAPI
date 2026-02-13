package net.liquidcars.ingestion.domain.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ParticipantAddressDto implements Serializable {
    private AddressTypeDto type;
    private PostalAddressDto address;
}
