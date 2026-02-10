package net.liquidcars.ingestion.application.service.parser.model.JSON;

import lombok.Data;

import java.io.Serializable;

@Data
public class ParticipantAddressJSONModel implements Serializable {
    private AddressTypeJSONModel type;
    private PostalAddressJSONModel address;
}
