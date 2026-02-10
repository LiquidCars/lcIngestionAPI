package net.liquidcars.ingestion.application.service.parser.model.XML;

import lombok.Data;

import java.io.Serializable;

@Data
public class ParticipantAddressXMLModel implements Serializable {
    private AddressTypeXMLModel type;
    private PostalAddressXMLModel address;
}
